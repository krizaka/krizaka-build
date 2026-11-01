package com.krizaka.users.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The password settings ({@code krizaka.users.password}).
 *
 * @param breachCheck whether a new password is checked against Pwned Passwords (on by default)
 * @param breachRangeUrl the range endpoint (k-anonymity: only the first 5 characters of the SHA-1
 *     leave the service)
 */
@ConfigurationProperties(prefix = "krizaka.users.password")
public record PasswordProperties(Boolean breachCheck, String breachRangeUrl) {

  /** Defaults: checked, against api.pwnedpasswords.com. */
  public PasswordProperties {
    breachCheck = breachCheck == null || breachCheck;
    breachRangeUrl =
        breachRangeUrl == null || breachRangeUrl.isBlank()
            ? "https://api.pwnedpasswords.com/range/"
            : breachRangeUrl.trim();
  }
}
