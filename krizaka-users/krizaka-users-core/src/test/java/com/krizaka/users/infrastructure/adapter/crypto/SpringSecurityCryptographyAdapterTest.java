package com.krizaka.users.infrastructure.adapter.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SpringSecurityCryptographyAdapterTest {

  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private SpringSecurityCryptographyAdapter cryptographyAdapter;

  @Test
  @DisplayName("hashToken generates correct SHA-256 hex string")
  void hashToken() {
    String token = "my-secret-token";
    String expectedHash =
        "ea5add57437cbf20af59034d7ed17968dcc56767b41965fcc5b376d45db8b4a3"; // SHA-256 of
    // "my-secret-token"

    String result = cryptographyAdapter.hashToken(token);

    assertThat(result).isEqualTo(expectedHash);
  }

  @Test
  @DisplayName("encodePassword delegates to PasswordEncoder")
  void encodePassword() {
    when(passwordEncoder.encode("password123")).thenReturn("encoded123");

    String result = cryptographyAdapter.encodePassword("password123");

    assertThat(result).isEqualTo("encoded123");
    verify(passwordEncoder).encode("password123");
  }

  @Test
  @DisplayName("matchesPassword delegates comparison to PasswordEncoder")
  void matchesPassword() {
    when(passwordEncoder.matches("raw", "encoded")).thenReturn(true);

    boolean result = cryptographyAdapter.matchesPassword("raw", "encoded");

    assertThat(result).isTrue();
    verify(passwordEncoder).matches("raw", "encoded");
  }
}
