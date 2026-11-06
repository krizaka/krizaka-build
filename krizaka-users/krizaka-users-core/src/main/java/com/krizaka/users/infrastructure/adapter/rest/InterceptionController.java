package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.IdentityService;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the {@code interceptions} resource: loading an interception schema and
 * resolving an active interception.
 *
 * <p>Replaces the former GraphQL {@code interceptionSchema} query and {@code resolveInterception}
 * mutation (REST-only transport consolidation, ADR-028).
 */
@RestController
@RequestMapping("/api/v1/interceptions")
public class InterceptionController {

  private final IdentityService identityService;

  public InterceptionController(IdentityService identityService) {
    this.identityService =
        Objects.requireNonNull(identityService, "IdentityService must not be null");
  }

  /**
   * Returns the raw interception schema JSON for the given schema id, served as text so the JSON
   * string is not double-encoded by Jackson (the client parses it).
   */
  @GetMapping("/{schemaId}")
  public ResponseEntity<String> getSchema(
      @PathVariable String schemaId, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(identityService.loadInterceptionSchema(schemaId));
  }

  /** Request payload for resolving an active interception. */
  public record ResolveInterceptionRequest(
      String interceptionType, String schemaId, Map<String, Object> responses) {}

  /** Resolves an active interception by merging the user's responses. */
  @PostMapping("/resolve")
  public ResponseEntity<Map<String, Object>> resolve(
      @RequestBody ResolveInterceptionRequest request, @AuthenticationPrincipal User user) {
    identityService.resolveInterception(
        user.id(), request.interceptionType(), request.schemaId(), request.responses());
    return ResponseEntity.ok(Map.of("resolved", true));
  }
}
