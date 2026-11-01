package com.krizaka.users.service.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import com.krizaka.users.domain.ports.inbound.LoginLockoutService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/** Wires the authentication throttle in front of the security chain (krizaka-users#17). */
@Configuration
@EnableConfigurationProperties(AuthRateLimitProperties.class)
public class AuthRateLimitConfig {

  /** The throttle runs before Spring Security: a refused request costs no password hashing. */
  @Bean
  public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilter(
      AuthRateLimitProperties props, LoginLockoutService lockout) {
    var registration =
        new FilterRegistrationBean<>(
            new AuthRateLimitFilter(props, Ticker.systemTicker(), lockout));
    registration.addUrlPatterns("/api/v1/auth/*");
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    return registration;
  }
}
