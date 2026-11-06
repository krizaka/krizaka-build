package com.krizaka.users.service.infrastructure.config;

import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Who may call the machine-to-machine surface ({@code krizaka.users.internal}, ADR-001).
 *
 * @param allowedCallers path pattern under {@code /internal/v1/} to the caller names (the token's
 *     {@code sub}) accepted there; a path with no entry stays open to any registered caller
 */
@ConfigurationProperties(prefix = "krizaka.users.internal")
public record InternalProperties(Map<String, Set<String>> allowedCallers) {

  public InternalProperties {
    allowedCallers = allowedCallers == null ? Map.of() : Map.copyOf(allowedCallers);
  }
}
