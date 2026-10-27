package com.krizaka.test.architecture.fixture.security.good;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * The compliant shape: prompt in a POST body, a GET with a paging parameter, a list in the
 * transaction.
 */
public class ChatEndpoint {

  private static final String LABEL = "chat";

  @PostMapping("/ask")
  public String ask(@RequestBody String prompt) {
    return prompt + LABEL;
  }

  @GetMapping("/history")
  public String history(@RequestParam("page") int page) {
    return "page " + page;
  }

  @Transactional(readOnly = true)
  public List<String> load() {
    return List.of();
  }

  public Stream<String> stream() {
    return Stream.empty();
  }
}
