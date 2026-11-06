package com.krizaka.users.service.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.krizaka.users.domain.ports.inbound.LoginLockoutService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

/** The wiring the deployed service runs with: no property set, then the deploy-style overrides. */
class AuthRateLimitWiringTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(AuthRateLimitConfig.class)
          .withBean(LoginLockoutService.class, () -> mock(LoginLockoutService.class));

  @Test
  void theContextStartsWithTheDefaultsAlone() {
    runner.run(
        ctx -> {
          assertThat(ctx).hasNotFailed();
          assertThat(ctx).hasSingleBean(FilterRegistrationBean.class);
          assertThat(ctx.getBean(AuthRateLimitProperties.class).login().requests()).isEqualTo(20);
        });
  }

  @Test
  void propertiesAreOverridableFromTheEnvironment() {
    runner
        .withPropertyValues(
            "krizaka.users-service.rate-limit.login.requests=7",
            "krizaka.users-service.rate-limit.login.window=PT30S",
            "krizaka.users-service.rate-limit.trusted-proxies=100.64.0.0/10,10.0.0.0/8",
            "krizaka.users-service.rate-limit.client-ip-header=DO-Connecting-IP")
        .run(
            ctx -> {
              assertThat(ctx).hasNotFailed();
              assertThat(ctx.getBean(AuthRateLimitProperties.class).login().requests())
                  .isEqualTo(7);
            });
  }
}
