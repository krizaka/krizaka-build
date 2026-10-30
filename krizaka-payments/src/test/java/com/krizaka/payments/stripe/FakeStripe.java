package com.krizaka.payments.stripe;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** A local stand-in for the processor's API: mocked HTTP, no network. */
final class FakeStripe implements AutoCloseable {

  /** One request received. */
  record Call(String method, String path, Map<String, String> form, Map<String, String> headers) {}

  /** One canned answer. */
  record Reply(int status, String json) {}

  private final HttpServer server;
  private final List<Call> calls = Collections.synchronizedList(new ArrayList<>());

  FakeStripe(Function<Call, Reply> answer) throws IOException {
    server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
    server.createContext(
        "/",
        exchange -> {
          String query = exchange.getRequestURI().getRawQuery();
          String body =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          Map<String, String> headers = new LinkedHashMap<>();
          exchange
              .getRequestHeaders()
              .forEach((k, v) -> headers.put(k.toLowerCase(java.util.Locale.ROOT), v.getFirst()));
          Call call =
              new Call(
                  exchange.getRequestMethod(),
                  exchange.getRequestURI().getPath(),
                  form(query == null || query.isEmpty() ? body : query),
                  headers);
          calls.add(call);
          Reply reply = answer.apply(call);
          byte[] out = reply.json().getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("content-type", "application/json");
          exchange.sendResponseHeaders(reply.status(), out.length);
          exchange.getResponseBody().write(out);
          exchange.close();
        });
    server.start();
  }

  String base() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  List<Call> calls() {
    return List.copyOf(calls);
  }

  private static Map<String, String> form(String encoded) {
    Map<String, String> out = new LinkedHashMap<>();
    for (String pair : encoded.split("&")) {
      if (pair.isEmpty()) {
        continue;
      }
      int eq = pair.indexOf('=');
      String key = URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), StandardCharsets.UTF_8);
      out.put(key, eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
    }
    return out;
  }

  @Override
  public void close() {
    server.stop(0);
  }
}
