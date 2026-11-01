package com.krizaka.users.infrastructure.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.web.client.RestClient;

/**
 * Spring {@link Configuration} for the {@code krizaka-users-core} module.
 *
 * <p>Declares infrastructure beans shared across identity services, starting with the password
 * encoder, which must be a singleton (re-instantiating it per call wastes CPU).
 *
 * <p>This class deliberately avoids auto-configuration starters ({@code
 * spring-boot-starter-security} auto-configuration) to remain compliant with AGENTS.md §1.A — No
 * Starter Leaks.
 *
 * @see com.krizaka.users.domain.ports.inbound.IdentityService
 */
@Configuration
@ConfigurationPropertiesScan
public class IdentityConfig {

  /**
   * The password encoder of the identity module (ADR-016 decision 2): new hashes are Argon2id
   * ({@code {argon2}…}); the existing BCrypt hashes (stored without a prefix, or {@code {bcrypt}})
   * and the products' scrypt hashes are still verified, and rehashed after a successful sign-in
   * ({@link PasswordEncoder#upgradeEncoding}). A singleton: the cost is paid per password, not per
   * construction.
   *
   * @return the delegating encoder, Argon2id by default (19 MiB, 2 passes, 1 lane: the OWASP floor)
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("argon2", new Argon2PasswordEncoder(16, 32, 1, 19_456, 2));
    encoders.put("bcrypt", new BCryptPasswordEncoder());
    encoders.put("scrypt", SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
    DelegatingPasswordEncoder delegating = new DelegatingPasswordEncoder("argon2", encoders);
    delegating.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
    return delegating;
  }

  /**
   * Default {@link RestClient} bean for identity HTTP operations.
   *
   * <p>Used by OAuth2 provider verifiers (GitHub, Google) for token validation calls. Per §2.16
   * [ERR-120], all HTTP calls use Spring {@link RestClient}.
   *
   * @return A default {@link RestClient} instance.
   */
  @Bean
  public RestClient identityRestClient() {
    return RestClient.create();
  }
}
