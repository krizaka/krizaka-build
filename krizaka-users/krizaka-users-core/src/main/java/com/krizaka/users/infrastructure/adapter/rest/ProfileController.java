package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.IdentityService;
import com.krizaka.users.infrastructure.adapter.rest.dto.UserDescriptor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the authenticated user's {@code profile} resource.
 *
 * <p>Replaces the former GraphQL {@code me} query and {@code updatePreferences} mutation (REST-only
 * transport consolidation, ADR-028). Identity domain models are mapped to {@link UserDescriptor}
 * DTOs at the boundary [ERR-102].
 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

  private final IdentityService identityService;

  public ProfileController(IdentityService identityService) {
    this.identityService =
        Objects.requireNonNull(identityService, "IdentityService must not be null");
  }

  /** Returns the authenticated user's profile. */
  @GetMapping
  public ResponseEntity<UserDescriptor> me(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(toDescriptor(user));
  }

  /** Updates the authenticated user's preference map and returns the refreshed profile. */
  @PutMapping("/preferences")
  public ResponseEntity<UserDescriptor> updatePreferences(
      @RequestBody Map<String, Object> preferences, @AuthenticationPrincipal User user) {
    User updated = identityService.updatePreferences(user.id().toString(), preferences);
    return ResponseEntity.ok(toDescriptor(updated));
  }

  private static UserDescriptor toDescriptor(User user) {
    return new UserDescriptor(
        user.id().toString(),
        user.username(),
        user.email(),
        List.copyOf(user.authorities()),
        user.preferences());
  }
}
