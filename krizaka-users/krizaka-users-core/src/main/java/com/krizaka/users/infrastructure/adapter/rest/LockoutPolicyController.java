package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.LockoutPolicy;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.LoginLockoutService;
import java.time.Duration;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The thresholds of the sign-in lockout for the operator console: administrators only, every change
 * audited. A figure that makes no sense answers 400 (the policy refuses it).
 */
@RestController
@RequestMapping("/api/v1/admin/lockout-policy")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class LockoutPolicyController {

  private final LoginLockoutService service;

  public LockoutPolicyController(LoginLockoutService service) {
    this.service = Objects.requireNonNull(service, "LoginLockoutService must not be null");
  }

  /** The policy as the API speaks it: durations in seconds. */
  public record PolicyBody(
      int threshold,
      int accountThreshold,
      long baseDelaySeconds,
      long maxDelaySeconds,
      long forgetAfterSeconds) {

    static PolicyBody of(LockoutPolicy p) {
      return new PolicyBody(
          p.threshold(),
          p.accountThreshold(),
          p.baseDelay().toSeconds(),
          p.maxDelay().toSeconds(),
          p.forgetAfter().toSeconds());
    }

    LockoutPolicy toPolicy() {
      return new LockoutPolicy(
          threshold,
          accountThreshold,
          Duration.ofSeconds(baseDelaySeconds),
          Duration.ofSeconds(maxDelaySeconds),
          Duration.ofSeconds(forgetAfterSeconds));
    }
  }

  @GetMapping
  public ResponseEntity<PolicyBody> get() {
    return ResponseEntity.ok(PolicyBody.of(service.policy()));
  }

  @PutMapping
  public ResponseEntity<PolicyBody> put(
      @AuthenticationPrincipal User admin, @RequestBody PolicyBody body) {
    var updated =
        service.updatePolicy(new AdminActor(admin.id().toString(), admin.email()), body.toPolicy());
    return ResponseEntity.ok(PolicyBody.of(updated));
  }
}
