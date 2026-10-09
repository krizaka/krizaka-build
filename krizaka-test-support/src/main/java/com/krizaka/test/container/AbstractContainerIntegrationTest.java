package com.krizaka.test.container;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The base class of every Krizaka integration test that needs a real PostgreSQL or RabbitMQ.
 *
 * <p>One container of each is started per JVM, the first time a subclass is loaded, and shared by
 * every test class of the run: a suite pays for the containers once, not once per class. Their
 * coordinates are handed to Spring through {@link DynamicPropertySource} — {@code
 * spring.datasource.*} and {@code spring.rabbitmq.*} — so a {@code @SpringBootTest} subclass needs
 * no profile, no port and no property of its own:
 *
 * <pre>{@code
 * @SpringBootTest
 * class OutboxRoundTripIT extends AbstractContainerIntegrationTest {
 *
 *   @Test
 *   void publishesWhatWasCommitted() { ... }
 * }
 * }</pre>
 *
 * <p>A test that builds its own context (or none) reads the containers through {@link #postgres()}
 * and {@link #rabbit()}.
 *
 * <p>Shared containers mean shared state: a test owns the rows and queues it creates and must not
 * assume an empty database or broker — name what it creates after itself, or clean up after it.
 */
public abstract class AbstractContainerIntegrationTest {

  /** The PostgreSQL image; the major version every Krizaka service runs on. */
  public static final String POSTGRES_IMAGE = "postgres:16-alpine";

  /** The RabbitMQ image; 4.x, where quorum queues are the default durable queue type. */
  public static final String RABBITMQ_IMAGE = "rabbitmq:4-management-alpine";

  private static final PostgreSQLContainer<?> POSTGRES;

  private static final RabbitMQContainer RABBITMQ;

  static {
    // docker-java's default Engine API version (1.32) is refused by recent Docker daemons, whose
    // minimum is 1.40+; an explicit setting from outside still wins.
    if (System.getProperty("api.version") == null && System.getenv("DOCKER_API_VERSION") == null) {
      System.setProperty("api.version", "1.43");
    }
    POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse(POSTGRES_IMAGE))
            .withDatabaseName("krizaka_test")
            .withUsername("krizaka")
            .withPassword("krizaka_test_pwd");
    // RabbitMQContainer's own readiness check: waiting for every exposed port would block on the
    // clustering ports a single node never opens.
    RABBITMQ = new RabbitMQContainer(DockerImageName.parse(RABBITMQ_IMAGE));
    POSTGRES.start();
    RABBITMQ.start();
  }

  /** For subclasses only. */
  protected AbstractContainerIntegrationTest() {}

  /**
   * Points Spring at the shared containers.
   *
   * @param registry the test context's dynamic properties
   */
  @DynamicPropertySource
  static void containerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
    registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
    registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
    registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
  }

  /**
   * The shared PostgreSQL container, started.
   *
   * @return the container, for a test that connects without Spring
   */
  protected static PostgreSQLContainer<?> postgres() {
    return POSTGRES;
  }

  /**
   * The shared RabbitMQ container, started.
   *
   * @return the container, for a test that connects without Spring
   */
  protected static RabbitMQContainer rabbit() {
    return RABBITMQ;
  }
}
