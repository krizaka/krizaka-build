package com.krizaka.users.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.EnvironmentRegistration;
import com.krizaka.users.domain.model.SocialConnection;
import com.krizaka.users.domain.ports.outbound.AdminUserRepositoryPort;
import com.krizaka.users.domain.ports.outbound.SecretSealPort;
import com.krizaka.users.domain.ports.outbound.SocialConnectionRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialConnectionServiceImplTest {

  private static final AdminActor SYSTEM = new AdminActor("system", "system");
  private static final EnvironmentRegistration GITHUB =
      new EnvironmentRegistration("github", "gh-id", "gh-clear-secret");

  @Mock SocialConnectionRepositoryPort connections;
  @Mock SecretSealPort vault;
  @Mock AdminUserRepositoryPort audit;
  SocialConnectionServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new SocialConnectionServiceImpl(connections, vault, audit);
  }

  @Test
  void importsOnceSealedAndAudited() {
    when(connections.find("github")).thenReturn(Optional.empty());
    when(vault.available()).thenReturn(true);
    when(vault.currentKeyId()).thenReturn("k1");
    when(vault.seal("gh-clear-secret")).thenReturn("v2.k1.a.b");

    assertThat(service.importFromEnvironment(SYSTEM, List.of(GITHUB))).isEqualTo(1);

    ArgumentCaptor<SocialConnection> saved = ArgumentCaptor.forClass(SocialConnection.class);
    verify(connections).save(saved.capture());
    assertThat(saved.getValue().sealedClientSecret()).isEqualTo("v2.k1.a.b");
    assertThat(saved.getValue().source()).isEqualTo("env");
    verify(audit)
        .appendAudit(
            eq("system"),
            eq("system"),
            eq("identity.social_connection_imported"),
            eq("github"),
            any());
  }

  @Test
  void doesNotImportTwice() {
    when(connections.find("github"))
        .thenReturn(
            Optional.of(new SocialConnection("github", "gh-id", "v2.k1.a.b", "k1", "env", null)));

    assertThat(service.importFromEnvironment(SYSTEM, List.of(GITHUB))).isZero();
    verify(connections, never()).save(any());
    verify(audit, never()).appendAudit(any(), any(), any(), any(), any());
  }

  @Test
  void withoutAVaultKeyNothingIsStoredInTheClear() {
    when(connections.find("github")).thenReturn(Optional.empty());
    when(vault.available()).thenReturn(false);

    assertThat(service.importFromEnvironment(SYSTEM, List.of(GITHUB))).isZero();
    verify(connections, never()).save(any());
  }

  @Test
  void skipsAProviderTheEnvironmentDoesNotConfigure() {
    var blank = new EnvironmentRegistration("google", "", "");
    assertThat(service.importFromEnvironment(SYSTEM, List.of(blank))).isZero();
    verify(connections, never()).save(any());
  }

  @Test
  void rewrapsOnlyWhatAnOlderKeyWraps() {
    when(vault.currentKeyId()).thenReturn("k2");
    when(vault.keyIdOf("v2.k1.a.b")).thenReturn("k1");
    when(vault.keyIdOf("v2.k2.c.d")).thenReturn("k2");
    when(vault.rewrap("v2.k1.a.b")).thenReturn("v2.k2.a2.b");
    when(connections.findAll())
        .thenReturn(
            List.of(
                new SocialConnection("github", "g", "v2.k1.a.b", "k1", "env", null),
                new SocialConnection("google", "o", "v2.k2.c.d", "k2", "env", null),
                new SocialConnection("none", "n", null, null, "admin", null)));

    assertThat(service.rewrapAll(new AdminActor("1", "root@example.com"))).isEqualTo(1);

    verify(connections).save(new SocialConnection("github", "g", "v2.k2.a2.b", "k2", "env", null));
    verify(audit)
        .appendAudit(
            eq("1"),
            eq("root@example.com"),
            eq("identity.social_connection_rewrapped"),
            eq("github"),
            any());
  }

  @Test
  void theSecretIsOpenedOnlyByClientSecret() {
    when(connections.find("github"))
        .thenReturn(
            Optional.of(new SocialConnection("github", "g", "v2.k1.a.b", "k1", "env", null)));
    when(vault.open("v2.k1.a.b")).thenReturn("gh-clear-secret");

    assertThat(service.clientSecret("github")).contains("gh-clear-secret");
    assertThat(service.list()).isEmpty();
  }

  @Test
  void theViewSaysSetOrNotSetAndNothingElse() {
    when(connections.findAll())
        .thenReturn(List.of(new SocialConnection("github", "g", "v2.k1.a.b", "k1", "env", null)));

    var view = service.list().getFirst();
    assertThat(view.secretSet()).isTrue();
    assertThat(view.toString()).doesNotContain("v2.k1");
  }
}
