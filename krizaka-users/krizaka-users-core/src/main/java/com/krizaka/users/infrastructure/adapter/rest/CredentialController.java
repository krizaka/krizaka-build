package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.IdentityService;
import com.krizaka.users.infrastructure.adapter.rest.dto.RotateCredentialRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.UserCredentialRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.UserCredentialResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user's own provider keys (BYOK), owner only: every route is scoped by the authenticated user,
 * so another user's key answers 404. Write-only: no response carries a key.
 */
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

  /** Stores a new key: 201, or 409 when the user already has one for the provider. */
  @PostMapping
  public ResponseEntity<UserCredentialResponse> createCredential(
      @RequestBody UserCredentialRequest request, @AuthenticationPrincipal User user) {
    identityService.createUserCredential(
        user.id().toString(), request.providerName(), request.apiKey());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new UserCredentialResponse(request.providerName(), true));
  }

  /** Replaces the key (rotation): 204, or 404 when the user has none for the provider. */
  @PutMapping("/{providerName}")
  public ResponseEntity<Void> rotateCredential(
      @PathVariable String providerName,
      @RequestBody RotateCredentialRequest request,
      @AuthenticationPrincipal User user) {
    identityService.rotateUserCredential(user.id().toString(), providerName, request.apiKey());
    return ResponseEntity.noContent().build();
  }

  /** Deletes the key, row and sealed value: 204, or 404 when there is none. */
  @DeleteMapping("/{providerName}")
  public ResponseEntity<Void> deleteCredential(
      @PathVariable String providerName, @AuthenticationPrincipal User user) {
    identityService.deleteUserCredential(user.id().toString(), providerName);
    return ResponseEntity.noContent().build();
  }
}
