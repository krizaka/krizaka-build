package com.krizaka.users.service.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

  private final ClientIpResolver resolver =
      new ClientIpResolver(List.of("10.0.0.0/8", "127.0.0.1"), "DO-Connecting-IP");

  private static MockHttpServletRequest req(String peer, String xff, String direct) {
    var r = new MockHttpServletRequest();
    r.setRemoteAddr(peer);
    if (xff != null) {
      r.addHeader("X-Forwarded-For", xff);
    }
    if (direct != null) {
      r.addHeader("DO-Connecting-IP", direct);
    }
    return r;
  }

  @Test
  void anUntrustedPeerIsTheClientAndItsHeadersAreIgnored() {
    assertThat(resolver.resolve(req("203.0.113.5", "1.2.3.4", "5.6.7.8"))).isEqualTo("203.0.113.5");
  }

  @Test
  void theRightmostUntrustedHopIsTheClient() {
    assertThat(resolver.resolve(req("10.0.0.5", "6.6.6.6, 203.0.113.9, 10.0.0.2", null)))
        .isEqualTo("203.0.113.9");
  }

  @Test
  void aTrustedPeerWithoutHeaderIsItself() {
    assertThat(resolver.resolve(req("10.0.0.5", null, null))).isEqualTo("10.0.0.5");
  }

  @Test
  void garbageInTheHeaderFallsBackToThePeer() {
    assertThat(resolver.resolve(req("10.0.0.5", "not-an-ip", null))).isEqualTo("10.0.0.5");
  }

  @Test
  void theConfiguredClientHeaderIsHonouredFromATrustedPeerOnly() {
    assertThat(resolver.resolve(req("10.0.0.5", "1.1.1.1", "198.51.100.4")))
        .isEqualTo("198.51.100.4");
    assertThat(resolver.resolve(req("203.0.113.5", null, "198.51.100.4"))).isEqualTo("203.0.113.5");
  }
}
