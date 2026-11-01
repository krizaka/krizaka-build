package com.krizaka.users.service.infrastructure.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

/**
 * The address a request really comes from, safe against a forged {@code X-Forwarded-For}.
 *
 * <p>The direct peer is the edge, which <em>appends</em> its own peer to whatever the client sent.
 * So the header is {@code <anything the client wrote>, <hops we can trust>}: it is read from the
 * right, skipping trusted proxies, and the first address that is not one is the client. A peer that
 * is not a trusted proxy is the client itself and its headers are ignored.
 */
final class ClientIpResolver {

  private final List<IpAddressMatcher> trusted;
  private final String clientIpHeader;

  ClientIpResolver(List<String> trustedProxies, String clientIpHeader) {
    this.trusted = trustedProxies.stream().map(String::trim).map(IpAddressMatcher::new).toList();
    this.clientIpHeader = clientIpHeader == null ? "" : clientIpHeader.trim();
  }

  String resolve(HttpServletRequest request) {
    String peer = request.getRemoteAddr();
    if (!isTrusted(peer)) {
      return peer;
    }
    if (!clientIpHeader.isEmpty()) {
      String direct = clean(request.getHeader(clientIpHeader));
      if (direct != null) {
        return direct;
      }
    }
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded == null || forwarded.isBlank()) {
      return peer;
    }
    String[] hops = forwarded.split(",");
    for (int i = hops.length - 1; i >= 0; i--) {
      String hop = clean(hops[i]);
      if (hop == null) {
        return peer;
      }
      if (!isTrusted(hop)) {
        return hop;
      }
    }
    // Every hop is a trusted proxy: the client is the first of them (an internal caller).
    String first = clean(hops[0]);
    return first != null ? first : peer;
  }

  private boolean isTrusted(String address) {
    for (IpAddressMatcher matcher : trusted) {
      try {
        if (matcher.matches(address)) {
          return true;
        }
      } catch (IllegalArgumentException ex) {
        return false;
      }
    }
    return false;
  }

  /** A literal IP address, or {@code null}: a hostname or garbage is never a client identity. */
  private static String clean(String raw) {
    if (raw == null) {
      return null;
    }
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (value.isEmpty() || value.length() > 45 || !value.matches("[0-9a-f:.]+")) {
      return null;
    }
    return value;
  }
}
