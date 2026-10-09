package com.krizaka.test.container;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;

class AbstractContainerIntegrationTestIT extends AbstractContainerIntegrationTest {

  @Test
  void bothContainersRunOncePerJvm() {
    assertThat(postgres().isRunning()).isTrue();
    assertThat(rabbit().isRunning()).isTrue();
  }

  @Test
  void theDatabaseAcceptsTheRegisteredCredentials() throws Exception {
    Map<String, Object> properties = registered();
    try (Connection connection =
            DriverManager.getConnection(
                (String) properties.get("spring.datasource.url"),
                (String) properties.get("spring.datasource.username"),
                (String) properties.get("spring.datasource.password"));
        ResultSet one = connection.createStatement().executeQuery("SELECT 1")) {
      assertThat(one.next()).isTrue();
    }
  }

  @Test
  void springIsPointedAtTheBroker() {
    Map<String, Object> properties = registered();
    assertThat(properties)
        .containsEntry("spring.rabbitmq.host", rabbit().getHost())
        .containsEntry("spring.rabbitmq.port", rabbit().getAmqpPort())
        .containsKeys("spring.rabbitmq.username", "spring.rabbitmq.password");
  }

  private static Map<String, Object> registered() {
    Map<String, Object> values = new HashMap<>();
    DynamicPropertyRegistry registry =
        (name, supplier) -> values.put(name, ((Supplier<?>) supplier).get());
    containerProperties(registry);
    return values;
  }
}
