package com.krizaka.test.architecture.fixture.security.bad;

import java.util.stream.Stream;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Reproduces conversation-service#19 (prompt on a GET event stream) and #10 (transactional stream).
 */
public class ChatEndpoint {

  @GetMapping(path = "/stream", produces = "text/event-stream")
  public Stream<String> stream(@RequestParam("conversation") String id) {
    return Stream.of(id);
  }

  @GetMapping("/ask")
  public String ask(@RequestParam("prompt") String prompt) {
    return prompt;
  }

  @Transactional(readOnly = true)
  public Stream<String> history() {
    return Stream.empty();
  }
}
