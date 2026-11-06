package com.krizaka.users.persistence.infrastructure.adapter.persistence.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** krizaka-users#28: the converter is a Spring bean fed by its context, never by a static. */
class CryptoConverterContextTest {

  private static final String SALT = "0123456789abcdef";

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
          .withUserConfiguration(CryptoConverter.class);

  @Test
  @DisplayName("Two contexts with different keys do not share state")
  void contextsAreIsolated() {
    runner
        .withPropertyValues(
            "krizaka.users.crypto.key=key-of-context-one-0123456789",
            "krizaka.users.crypto.salt=" + SALT)
        .run(
            one ->
                runner
                    .withPropertyValues(
                        "krizaka.users.crypto.key=key-of-context-two-9876543210",
                        "krizaka.users.crypto.salt=" + SALT)
                    .run(
                        two -> {
                          CryptoConverter a = one.getBean(CryptoConverter.class);
                          CryptoConverter b = two.getBean(CryptoConverter.class);
                          String secret = a.convertToDatabaseColumn("secret");
                          assertThat(a.convertToEntityAttribute(secret)).isEqualTo("secret");
                          assertThatThrownBy(() -> b.convertToEntityAttribute(secret))
                              .isInstanceOf(RuntimeException.class);
                        }));
  }

  @Test
  @DisplayName("A missing key fails the startup, whatever the environment holds")
  void missingKeyFailsClosed() {
    runner
        .withPropertyValues("krizaka.users.crypto.salt=" + SALT)
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  @Test
  @DisplayName("A blank key or salt fails the startup")
  void blankFailsClosed() {
    runner
        .withPropertyValues("krizaka.users.crypto.key=", "krizaka.users.crypto.salt=" + SALT)
        .run(ctx -> assertThat(ctx).hasFailed());
    runner
        .withPropertyValues(
            "krizaka.users.crypto.key=some-key-0123456789", "krizaka.users.crypto.salt= ")
        .run(ctx -> assertThat(ctx).hasFailed());
  }
}
