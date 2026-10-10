package com.krizaka.test.events;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class of an event contract test: the JSON an event travels as must validate against the JSON
 * Schema its {@code -api} module publishes.
 *
 * <p>A module that publishes events ships one schema per routing key and version, on its classpath
 * at {@code events/<routing-key>.v<version>.json} (JSON Schema draft 2020-12) — {@code
 * krizaka-users-api} carries {@code events/evt.user.registered.v1.json}. Two kinds of test extend
 * this class:
 *
 * <ul>
 *   <li><b>the producer</b> serialises the event it publishes, with the mapper its outbox uses, and
 *       {@link #assertConforms asserts it conforms}: a renamed field fails the producer's build,
 *       not a consumer in production;
 *   <li><b>a consumer that keeps a copy of the contract</b> (its own record, never the producer's
 *       class) depends on the producer's {@code -api} with {@code <scope>test</scope>} and {@link
 *       #assertReadable asserts} that a document the schema accepts reads into its copy.
 * </ul>
 *
 * <pre>{@code
 * class UserRegisteredContractTest extends EventContractTest {
 *   @Test
 *   void theEventConformsToItsSchema() {
 *     assertConforms("evt.user.registered", 1, new UserRegisteredEvent(user, "token"));
 *   }
 * }
 * }</pre>
 *
 * <p>A schema is the public contract of an event: a change that a consumer cannot read is a new
 * version ({@code v2}) under a new routing key, never an edit of {@code v1}.
 */
public abstract class EventContractTest {

  /** The schema dialect every event schema declares in {@code $schema}. */
  public static final String DIALECT = SpecificationVersion.DRAFT_2020_12.getDialectId();

  private static final SchemaRegistry REGISTRY =
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

  private static final JsonMapper DEFAULT_MAPPER = JsonMapper.builder().build();

  /** Creates the test. */
  protected EventContractTest() {}

  /**
   * The mapper an event is serialised and read with. Jackson 3's defaults — ISO-8601 instants,
   * enums by name — which are Spring Boot's; override it when the outbox writes with another
   * mapper.
   *
   * @return the mapper
   */
  protected JsonMapper jsonMapper() {
    return DEFAULT_MAPPER;
  }

  /**
   * Where the schema of an event lives on the classpath.
   *
   * @param routingKey the routing key, such as {@code evt.user.registered}
   * @param version the contract version, from 1
   * @return {@code events/<routingKey>.v<version>.json}
   */
  public static String schemaPath(String routingKey, int version) {
    Objects.requireNonNull(routingKey, "routingKey");
    if (version < 1) {
      throw new IllegalArgumentException("an event contract version starts at 1: " + version);
    }
    return "events/" + routingKey + ".v" + version + ".json";
  }

  /**
   * Loads the published schema of an event.
   *
   * @param routingKey the routing key
   * @param version the contract version
   * @return the schema
   * @throws AssertionError when the classpath has no such schema or it does not declare {@link
   *     #DIALECT}
   */
  protected Schema schema(String routingKey, int version) {
    String path = schemaPath(routingKey, version);
    JsonNode document;
    try (InputStream in = resource(path)) {
      if (in == null) {
        throw new AssertionError(
            "No schema for " + routingKey + " v" + version + " on the classpath (" + path + ")");
      }
      document = jsonMapper().readTree(in);
    } catch (IOException e) {
      throw new UncheckedIOException(path, e);
    }
    JsonNode dialect = document.get("$schema");
    if (dialect == null || !DIALECT.equals(dialect.asString())) {
      throw new AssertionError(path + " must declare \"$schema\": \"" + DIALECT + "\"");
    }
    return REGISTRY.getSchema(document);
  }

  /**
   * Asserts that an event, serialised with {@link #jsonMapper()}, conforms to its schema.
   *
   * @param routingKey the routing key the event is published under
   * @param version the contract version
   * @param event the event, as the producer builds it
   * @return the JSON document that was validated
   * @throws AssertionError listing every violation
   */
  protected String assertConforms(String routingKey, int version, Object event) {
    Objects.requireNonNull(event, "event");
    String json = jsonMapper().writeValueAsString(event);
    assertJsonConforms(routingKey, version, json);
    return json;
  }

  /**
   * Asserts that a JSON document conforms to an event's schema.
   *
   * @param routingKey the routing key
   * @param version the contract version
   * @param json the document
   * @throws AssertionError listing every violation
   */
  protected void assertJsonConforms(String routingKey, int version, String json) {
    List<Error> errors = schema(routingKey, version).validate(json, InputFormat.JSON);
    if (!errors.isEmpty()) {
      throw new AssertionError(
          routingKey
              + " v"
              + version
              + " does not conform to "
              + schemaPath(routingKey, version)
              + ":\n"
              + errors.stream()
                  .map(e -> "  - " + e.getInstanceLocation() + ": " + e.getMessage())
                  .collect(Collectors.joining("\n"))
              + "\n"
              + json);
    }
  }

  /**
   * Asserts that a document the producer's schema accepts reads into a consumer's copy of the
   * contract.
   *
   * @param routingKey the routing key
   * @param version the contract version
   * @param json an example of the event, valid against the schema
   * @param copy the consumer's own type for the event
   * @param <T> the consumer's type
   * @return the event as the consumer reads it, for further assertions
   * @throws AssertionError when the example is not valid, or the copy cannot read it
   */
  protected <T> T assertReadable(String routingKey, int version, String json, Class<T> copy) {
    assertJsonConforms(routingKey, version, json);
    try {
      return jsonMapper().readValue(json, copy);
    } catch (RuntimeException e) {
      throw new AssertionError(
          copy.getName() + " cannot read a valid " + routingKey + " v" + version + ": " + e, e);
    }
  }

  private InputStream resource(String path) {
    ClassLoader loader = getClass().getClassLoader();
    return loader == null
        ? ClassLoader.getSystemResourceAsStream(path)
        : loader.getResourceAsStream(path);
  }
}
