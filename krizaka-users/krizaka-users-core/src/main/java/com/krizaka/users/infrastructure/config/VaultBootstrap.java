package com.krizaka.users.infrastructure.config;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.EnvironmentRegistration;
import com.krizaka.users.domain.ports.inbound.SocialConnectionService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * At startup, imports the OAuth clients of the environment once as sealed connections (ADR-016
 * decision 6), then re-wraps what an older vault key still wraps. Both are no-ops when there is
 * nothing to do, and audited as {@code system} when they act.
 */
@Configuration
class VaultBootstrap {

  private static final Logger LOG = LoggerFactory.getLogger(VaultBootstrap.class);
  private static final AdminActor SYSTEM = new AdminActor("system", "system");

  @Bean
  ApplicationRunner sealIdentitySecrets(
      SocialConnectionService service, OAuthClientProperties clients) {
    return args -> {
      List<EnvironmentRegistration> registrations =
          clients.registration().entrySet().stream()
              .map(
                  e ->
                      new EnvironmentRegistration(
                          e.getKey(), e.getValue().clientId(), e.getValue().clientSecret()))
              .toList();
      int imported = service.importFromEnvironment(SYSTEM, registrations);
      int rewrapped = service.rewrapAll(SYSTEM);
      if (imported + rewrapped > 0) {
        LOG.info("Identity vault: {} imported, {} re-wrapped", imported, rewrapped);
      }
    };
  }
}
