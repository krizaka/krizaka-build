package com.krizaka.users.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.krizaka.users.domain.port.UserDirectoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class UsersClientAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(UsersClientAutoConfiguration.class));

  @Test
  void contributesNothingWithoutABaseUrl() {
    runner.run(context -> assertThat(context).doesNotHaveBean(UserDirectoryClient.class));
  }

  @Test
  void contributesTheHttpClientWhenConfigured() {
    runner
        .withPropertyValues(
            "krizaka.users.client.base-url=http://users:8083",
            "krizaka.users.client.service-secret=krizaka-test-secret-at-least-32-characters!",
            "krizaka.users.client.service-name=billing-service")
        .run(
            context ->
                assertThat(context.getBean(UserDirectoryClient.class))
                    .isInstanceOf(HttpUserDirectoryClient.class));
  }

  @Test
  void refusesToStartWithoutTheServiceIdentity() {
    runner
        .withPropertyValues("krizaka.users.client.base-url=http://users:8083")
        .run(context -> assertThat(context).hasFailed());
  }

  @Test
  void neverPrintsTheSecret() {
    String secret = "krizaka-test-secret-at-least-32-characters!";
    assertThat(
            new UsersClientProperties("http://u", secret, "svc", java.time.Duration.ZERO)
                .toString())
        .doesNotContain(secret);
  }
}
