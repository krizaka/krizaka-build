package com.krizaka.users.infrastructure.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.krizaka.users.domain.exception.CredentialConflictException;
import com.krizaka.users.domain.exception.CredentialNotFoundException;
import com.krizaka.users.domain.exception.SecurityGuardrailException;
import com.krizaka.users.domain.model.UserCredential;
import com.krizaka.users.domain.ports.outbound.SecretSealPort;
import com.krizaka.users.persistence.domain.model.UserCredentialDto;
import com.krizaka.users.persistence.domain.ports.UserCredentialPersistenceProvider;
import com.krizaka.users.persistence.infrastructure.adapter.persistence.converter.CryptoConverter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * krizaka-internal#2359 slice 1a: a provider key is sealed bound to its user and provider before it
 * is stored, an old AES-CBC row is re-sealed on its first read, a second key is a conflict, a
 * missing one is not found — and no message ever carries the key.
 */
@ExtendWith(MockitoExtension.class)
class UserCredentialAdapterTest {

  private static final String KEY = "not-a-real-provider-key";

  @Mock private UserCredentialPersistenceProvider provider;

  private final CryptoConverter legacy =
      new CryptoConverter("test-crypto-key-0123456789abcdef!", "0123456789abcdef");
  private UserCredentialAdapter adapter;

  /**
   * A vault whose sealed value is a v2 envelope that only opens for the context it was bound to.
   */
  private static final class BoundVault implements SecretSealPort {
    private static String b64(String s) {
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean available() {
      return true;
    }

    @Override
    public String currentKeyId() {
      return "k1";
    }

    @Override
    public String seal(String secret) {
      return seal(secret, "body");
    }

    @Override
    public String seal(String secret, String context) {
      return "v2.k1." + b64(context) + "." + b64(new StringBuilder(secret).reverse().toString());
    }

    @Override
    public String open(String sealed) {
      return open(sealed, "body");
    }

    @Override
    public String open(String sealed, String context) {
      String[] p = sealed.split("\\.");
      if (!p[2].equals(b64(context))) {
        throw new SecurityGuardrailException("The sealed value failed its integrity check");
      }
      String body = new String(Base64.getUrlDecoder().decode(p[3]), StandardCharsets.UTF_8);
      return new StringBuilder(body).reverse().toString();
    }

    @Override
    public String keyIdOf(String sealed) {
      return "k1";
    }

    @Override
    public String rewrap(String sealed) {
      return sealed;
    }
  }

  @BeforeEach
  void setUp() {
    adapter = new UserCredentialAdapter(provider, new BoundVault(), legacy);
  }

  @Test
  void createStoresTheKeySealedNeverInClear() {
    when(provider.insert(any())).thenAnswer(i -> Optional.of(i.getArgument(0)));

    adapter.create("user-1", "runway", KEY);

    ArgumentCaptor<UserCredentialDto> stored = ArgumentCaptor.forClass(UserCredentialDto.class);
    verify(provider).insert(stored.capture());
    assertThat(stored.getValue().apiKey()).startsWith("v2.").doesNotContain(KEY);
    assertThat(stored.getValue().toString()).doesNotContain(KEY).doesNotContain("v2.");
  }

  @Test
  void aSecondKeyForTheSameProviderIsAConflictThatNamesNoKey() {
    when(provider.insert(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> adapter.create("user-1", "runway", KEY))
        .isInstanceOf(CredentialConflictException.class)
        .message()
        .doesNotContain(KEY);
  }

  @Test
  void theStoredKeyOpensOnlyForItsOwnUserAndProvider() {
    when(provider.insert(any())).thenAnswer(i -> Optional.of(i.getArgument(0)));
    adapter.create("user-1", "runway", KEY);
    ArgumentCaptor<UserCredentialDto> stored = ArgumentCaptor.forClass(UserCredentialDto.class);
    verify(provider).insert(stored.capture());
    String sealed = stored.getValue().apiKey();

    when(provider.findByUserIdAndProviderName("user-1", "runway"))
        .thenReturn(Optional.of(new UserCredentialDto(1L, "user-1", "runway", sealed)));
    assertThat(adapter.findApiKeyByUserIdAndProviderName("user-1", "runway")).contains(KEY);

    // The same cipher text moved to another user's row does not open there.
    when(provider.findByUserIdAndProviderName("user-2", "runway"))
        .thenReturn(Optional.of(new UserCredentialDto(2L, "user-2", "runway", sealed)));
    assertThatThrownBy(() -> adapter.findApiKeyByUserIdAndProviderName("user-2", "runway"))
        .isInstanceOf(SecurityGuardrailException.class)
        .message()
        .doesNotContain(KEY);
  }

  @Test
  void anOldCbcRowIsReSealedOnItsFirstRead() {
    String cbc = legacy.convertToDatabaseColumn(KEY);
    when(provider.findByUserIdAndProviderName("user-1", "openai"))
        .thenReturn(Optional.of(new UserCredentialDto(1L, "user-1", "openai", cbc)));

    assertThat(adapter.findApiKeyByUserIdAndProviderName("user-1", "openai")).contains(KEY);

    ArgumentCaptor<String> resealed = ArgumentCaptor.forClass(String.class);
    verify(provider).replaceStoredKey(eq("user-1"), eq("openai"), resealed.capture());
    assertThat(resealed.getValue()).startsWith("v2.").doesNotContain(KEY);
  }

  @Test
  void aSealedRowIsNotRewrittenOnRead() {
    String sealed = new BoundVault().seal(KEY, "user-1|runway");
    when(provider.findByUserIdAndProviderName("user-1", "runway"))
        .thenReturn(Optional.of(new UserCredentialDto(1L, "user-1", "runway", sealed)));

    adapter.findApiKeyByUserIdAndProviderName("user-1", "runway");

    verify(provider, never()).replaceStoredKey(anyString(), anyString(), anyString());
  }

  @Test
  void rotateReplacesTheSealedKeyOrIsNotFound() {
    when(provider.replaceStoredKey(eq("user-1"), eq("runway"), anyString())).thenReturn(true);
    adapter.rotate("user-1", "runway", KEY);
    ArgumentCaptor<String> sealed = ArgumentCaptor.forClass(String.class);
    verify(provider).replaceStoredKey(eq("user-1"), eq("runway"), sealed.capture());
    assertThat(sealed.getValue()).startsWith("v2.").doesNotContain(KEY);

    when(provider.replaceStoredKey(eq("user-2"), eq("runway"), anyString())).thenReturn(false);
    assertThatThrownBy(() -> adapter.rotate("user-2", "runway", KEY))
        .isInstanceOf(CredentialNotFoundException.class)
        .message()
        .doesNotContain(KEY);
  }

  @Test
  void deleteRemovesTheRowOrIsNotFound() {
    when(provider.deleteByUserIdAndProviderName("user-1", "runway")).thenReturn(true);
    adapter.delete("user-1", "runway");
    verify(provider).deleteByUserIdAndProviderName("user-1", "runway");

    // Another user's key, or none: 404, the row is not revealed.
    when(provider.deleteByUserIdAndProviderName("user-2", "runway")).thenReturn(false);
    assertThatThrownBy(() -> adapter.delete("user-2", "runway"))
        .isInstanceOf(CredentialNotFoundException.class);
  }

  @Test
  void theListCarriesNoKey() {
    when(provider.findByUserId("user-1"))
        .thenReturn(List.of(new UserCredentialDto(1L, "user-1", "runway", "v2.k1.a.b")));

    List<UserCredential> result = adapter.findByUserId("user-1");

    assertThat(result).containsExactly(new UserCredential("runway", true));
  }
}
