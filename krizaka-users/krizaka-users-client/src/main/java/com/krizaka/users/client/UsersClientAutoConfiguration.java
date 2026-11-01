package com.krizaka.users.client;

import com.krizaka.users.domain.port.UserDirectoryClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Contributes a {@link UserDirectoryClient} when {@code krizaka.users.client.base-url} is set.
 *
 * <pre>{@code
 * krizaka:
 *   users:
 *     client:
 *       base-url: http://users:8083
 *       service-secret: ${IDENTITY_JWT_SECRET}
 *       service-name: billing-service
 * }</pre>
 */
@AutoConfiguration(
    afterName = "org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration")
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(prefix = "krizaka.users.client", name = "base-url")
@EnableConfigurationProperties(UsersClientProperties.class)
public class UsersClientAutoConfiguration {

  /**
   * The HTTP user directory.
   *
   * @param restClientBuilder the application's builder (observability, timeouts), when it has one
   * @param properties where and how to call the users service
   * @return the client
   */
  @Bean
  @ConditionalOnMissingBean(UserDirectoryClient.class)
  public UserDirectoryClient userDirectoryClient(
      ObjectProvider<RestClient.Builder> restClientBuilder, UsersClientProperties properties) {
    return new HttpUserDirectoryClient(
        restClientBuilder.getIfAvailable(RestClient::builder), properties);
  }
}
