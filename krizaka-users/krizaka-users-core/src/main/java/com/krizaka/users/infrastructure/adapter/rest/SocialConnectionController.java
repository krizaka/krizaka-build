package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.SocialConnectionView;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.SocialConnectionService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The identity-provider connections for the operator console: administrators only. The answer says
 * « secret set » or not; no secret, sealed or clear, ever leaves the service.
 */
@RestController
@RequestMapping("/api/v1/admin/social-connections")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class SocialConnectionController {

  private final SocialConnectionService service;

  public SocialConnectionController(SocialConnectionService service) {
    this.service = Objects.requireNonNull(service, "SocialConnectionService must not be null");
  }

  @GetMapping
  public ResponseEntity<List<SocialConnectionView>> list() {
    return ResponseEntity.ok(service.list());
  }

  /** Re-wraps every secret under the current vault key (after a key change). */
  @PostMapping("/rewrap")
  public ResponseEntity<Map<String, Integer>> rewrap(@AuthenticationPrincipal User admin) {
    int done = service.rewrapAll(new AdminActor(admin.id().toString(), admin.email()));
    return ResponseEntity.ok(Map.of("rewrapped", done));
  }
}
