package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.infrastructure.config.IssuerKeys;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The issuer's public keys (ADR-001): verifiers point {@code krizaka.security.jwt.jwk-set-uri}
 * here. Public by design — only public members are ever published, the current kid and the retired
 * ones, so a rotation never invalidates tokens in flight.
 */
@RestController
public class JwksController {

  private final IssuerKeys keys;

  public JwksController(IssuerKeys keys) {
    this.keys = keys;
  }

  @GetMapping(value = "/.well-known/jwks.json", produces = "application/json")
  public ResponseEntity<Map<String, Object>> jwks() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
        .body(keys.publicSet().toJSONObject(true));
  }
}
