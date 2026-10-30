package com.krizaka.payments.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.krizaka.payments.PaymentEvent;
import com.krizaka.payments.PaymentsException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The cases shared with {@code @krizaka/payments}: the same input gives the same answer in both
 * languages.
 */
class ContractTest {

  private static final JsonObject CONTRACT = load();

  private static JsonObject load() {
    try (InputStream in =
        ContractTest.class.getResourceAsStream("/contract/payments-contract.json")) {
      return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
          .getAsJsonObject();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @TestFactory
  List<DynamicTest> translatesEveryEventLikeTheTypeScriptPackage() {
    return stream(CONTRACT.getAsJsonArray("translate"))
        .map(
            c ->
                DynamicTest.dynamicTest(
                    c.get("name").getAsString(),
                    () -> {
                      JsonObject event = new JsonObject();
                      event.addProperty("id", "evt_9");
                      event.addProperty("type", c.get("type").getAsString());
                      JsonObject data = new JsonObject();
                      data.add("object", c.get("object"));
                      event.add("data", data);
                      assertThat(asJson(WebhookTranslator.translate(event.toString())))
                          .isEqualTo(c.getAsJsonObject("expected"));
                    }))
        .toList();
  }

  @TestFactory
  List<DynamicTest> verifiesEverySignatureLikeTheTypeScriptPackage() {
    String body = "{\"id\":\"evt_1\",\"type\":\"x\",\"data\":{\"object\":{}}}";
    String secret = CONTRACT.get("secret").getAsString();
    return stream(CONTRACT.getAsJsonArray("signature"))
        .map(
            c ->
                DynamicTest.dynamicTest(
                    c.get("name").getAsString(),
                    () -> {
                      String header = header(c, body);
                      Instant now = Instant.ofEpochSecond(c.get("now").getAsLong());
                      if (c.get("valid").getAsBoolean()) {
                        assertThatCode(() -> WebhookSignature.verify(body, header, secret, now))
                            .doesNotThrowAnyException();
                      } else {
                        assertThatThrownBy(() -> WebhookSignature.verify(body, header, secret, now))
                            .isInstanceOfSatisfying(
                                PaymentsException.class,
                                e -> assertThat(e.status()).isEqualTo(400));
                      }
                    }))
        .toList();
  }

  private static @Nullable String header(JsonObject c, String body) {
    if (c.has("header")) {
      return c.get("header").isJsonNull() ? null : c.get("header").getAsString();
    }
    String signed =
        WebhookSignature.sign(
            body, c.get("signedWith").getAsString(), c.get("signedAt").getAsLong());
    return c.has("rotation") ? signed.replace("v1=", "v1=00,v1=") : signed;
  }

  private static Stream<JsonObject> stream(JsonArray array) {
    return array.asList().stream().map(JsonElement::getAsJsonObject);
  }

  private static JsonObject asJson(PaymentEvent event) {
    JsonObject o = new JsonObject();
    o.addProperty("id", event.id());
    o.addProperty("type", event.type());
    switch (event) {
      case PaymentEvent.PaymentPaid e -> {
        o.addProperty("kind", e.kind());
        o.addProperty("accountId", e.accountId());
        put(o, "customerId", e.customerId());
      }
      case PaymentEvent.SubscriptionStarted e -> {
        o.addProperty("accountId", e.accountId());
        put(o, "customerId", e.customerId());
        put(o, "subscriptionId", e.subscriptionId());
      }
      case PaymentEvent.SubscriptionChanged e -> {
        put(o, "accountId", e.accountId());
        put(o, "customerId", e.customerId());
        o.addProperty("subscriptionId", e.subscriptionId());
        o.addProperty("status", e.status());
        o.addProperty("active", e.active());
        o.add(
            "periodEnd",
            e.periodEnd() == null
                ? JsonNull.INSTANCE
                : JsonParser.parseString(e.periodEnd().toString()));
      }
      case PaymentEvent.Ignored e -> {
        // id and type only
      }
    }
    return o;
  }

  private static void put(JsonObject o, String key, @Nullable String value) {
    if (value == null) {
      o.add(key, JsonNull.INSTANCE);
    } else {
      o.addProperty(key, value);
    }
  }
}
