package com.krizaka.users.service.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** ADR-085, slice 1: the edge's request id reaches the audit through the logging context. */
class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void aWellFormedIdIsPutInTheLoggingContextForTheCallAndRemovedAfter() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "9f1c2d3e-aaaa-bbbb-cccc-123456789abc");
    var response = new MockHttpServletResponse();
    String[] seen = new String[1];
    FilterChain chain = (req, res) -> seen[0] = MDC.get("requestId");

    filter.doFilter(request, response, chain);

    assertThat(seen[0]).isEqualTo("9f1c2d3e-aaaa-bbbb-cccc-123456789abc");
    assertThat(MDC.get("requestId")).isNull();
    assertThat(response.getHeader("X-Request-Id"))
        .isEqualTo("9f1c2d3e-aaaa-bbbb-cccc-123456789abc");
  }

  @Test
  void aCallWithoutAnIdGetsOne() throws Exception {
    var response = new MockHttpServletResponse();
    String[] seen = new String[1];

    filter.doFilter(
        new MockHttpServletRequest(), response, (req, res) -> seen[0] = MDC.get("requestId"));

    assertThat(seen[0]).hasSize(36);
    assertThat(response.getHeader("X-Request-Id")).isEqualTo(seen[0]);
  }

  @Test
  void aMalformedIdIsReplaced() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-Id", "bad id\\nwith newline");
    String[] seen = new String[1];

    filter.doFilter(
        request, new MockHttpServletResponse(), (req, res) -> seen[0] = MDC.get("requestId"));

    assertThat(seen[0]).hasSize(36);
  }
}
