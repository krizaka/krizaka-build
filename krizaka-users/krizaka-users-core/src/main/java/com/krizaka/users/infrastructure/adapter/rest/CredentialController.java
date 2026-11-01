package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.IdentityService;
import com.krizaka.users.infrastructure.adapter.rest.dto.UserCredentialRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.UserCredentialResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller managing dynamic user AI provider credentials. */
@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialController {

  private final IdentityService identityService;

  public CredentialController(IdentityService identityService) {
    this.identityService =
        Objects.requireNonNull(identityService, "IdentityService must not be null");
  }

  @GetMapping
  public ResponseEntity<List<UserCredentialResponse>> getCredentials(
      @AuthenticationPrincipal User user) {
    List<UserCredentialResponse> responses =
        identityService.getUserCredentials(user.id().toString()).stream()
            .map(c -> new UserCredentialResponse(c.providerName(), c.configured()))
            .toList();
    return ResponseEntity.ok(responses);
  }

  @PostMapping
  public ResponseEntity<Void> saveCredential(
      @RequestBody UserCredentialRequest request, @AuthenticationPrincipal User user) {
    identityService.saveUserCredential(
        user.id().toString(), request.providerName(), request.apiKey());
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{providerName}")
  public ResponseEntity<Void> deleteCredential(
      @PathVariable String providerName, @AuthenticationPrincipal User user) {
    identityService.deleteUserCredential(user.id().toString(), providerName);
    return ResponseEntity.ok().build();
  }
}
