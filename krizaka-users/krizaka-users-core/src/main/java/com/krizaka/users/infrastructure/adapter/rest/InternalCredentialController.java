package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.CredentialGrantService;
import com.krizaka.users.infrastructure.adapter.rest.dto.CredentialUseRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.InternalCredentialResponse;
import java.util.Objects;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The one way a decrypted provider key leaves the identity context (GHSA-3pf7-rgf2-4x3m): a service
 * redeems a run-bound grant once. A service token alone is refused (403), whatever user it names.
 */
@RestController
@RequestMapping("/internal/v1/credentials")
class InternalCredentialController {

  private final CredentialGrantService grants;

  InternalCredentialController(CredentialGrantService grants) {
    this.grants = Objects.requireNonNull(grants, "CredentialGrantService must not be null");
  }

  /** 200 with the key; 403 when the grant is refused; 404 when the key was deleted since. */
  @PostMapping("/{providerName}/use")
  public ResponseEntity<InternalCredentialResponse> use(
      @PathVariable String providerName,
      @RequestBody CredentialUseRequest request,
      @AuthenticationPrincipal User caller) {
    String key = grants.redeem(request.grant(), providerName, request.runId(), caller.username());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(new InternalCredentialResponse(key));
  }
}
