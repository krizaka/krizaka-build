package com.krizaka.users.infrastructure.adapter.rest;

import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.model.OrganizationActor;
import com.krizaka.users.domain.model.OrganizationRole;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.OrganizationService;
import com.krizaka.users.infrastructure.adapter.rest.dto.AttachServiceAccountRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.CreateOrganizationRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.OrganizationMemberRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.OrganizationMemberResponse;
import com.krizaka.users.infrastructure.adapter.rest.dto.OrganizationResponse;
import com.krizaka.users.infrastructure.adapter.rest.dto.OrganizationServiceKeyResponse;
import java.util.List;
import java.util.Locale;
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
 * REST controller of organizations that own accounts: their members, the service accounts of the
 * products, and one view of those accounts' keys, which an organization administrator can revoke.
 */
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

  private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

  private final OrganizationService service;

  public OrganizationController(OrganizationService service) {
    this.service = Objects.requireNonNull(service, "OrganizationService must not be null");
  }

  private static OrganizationActor actor(User user) {
    return new OrganizationActor(
        user.id().toString(), user.authorities().contains(ADMIN_AUTHORITY));
  }

  @GetMapping
  public ResponseEntity<List<OrganizationResponse>> mine(@AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        service.listMine(actor(user)).stream().map(OrganizationResponse::from).toList());
  }

  @PostMapping
  public ResponseEntity<OrganizationResponse> create(
      @RequestBody CreateOrganizationRequest request, @AuthenticationPrincipal User user) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            OrganizationResponse.from(service.create(actor(user), request.slug(), request.name())));
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrganizationResponse> get(
      @PathVariable String id, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(OrganizationResponse.from(service.get(actor(user), id)));
  }

  @GetMapping("/{id}/members")
  public ResponseEntity<List<OrganizationMemberResponse>> members(
      @PathVariable String id, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        service.members(actor(user), id).stream().map(OrganizationMemberResponse::from).toList());
  }

  @PutMapping("/{id}/members")
  public ResponseEntity<OrganizationMemberResponse> addMember(
      @PathVariable String id,
      @RequestBody OrganizationMemberRequest request,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        OrganizationMemberResponse.from(
            service.addMember(actor(user), id, request.userId(), role(request.role()))));
  }

  @DeleteMapping("/{id}/members/{userId}")
  public ResponseEntity<Void> removeMember(
      @PathVariable String id, @PathVariable String userId, @AuthenticationPrincipal User user) {
    return service.removeMember(actor(user), id, userId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  @PostMapping("/{id}/service-accounts")
  public ResponseEntity<OrganizationMemberResponse> attachServiceAccount(
      @PathVariable String id,
      @RequestBody AttachServiceAccountRequest request,
      @AuthenticationPrincipal User user) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            OrganizationMemberResponse.from(
                service.attachServiceAccount(actor(user), id, request.userId())));
  }

  @GetMapping("/{id}/service-keys")
  public ResponseEntity<List<OrganizationServiceKeyResponse>> serviceKeys(
      @PathVariable String id, @AuthenticationPrincipal User user) {
    return ResponseEntity.ok(
        service.serviceKeys(actor(user), id).stream()
            .map(OrganizationServiceKeyResponse::from)
            .toList());
  }

  @DeleteMapping("/{id}/service-keys/{keyId}")
  public ResponseEntity<Void> revokeServiceKey(
      @PathVariable String id, @PathVariable String keyId, @AuthenticationPrincipal User user) {
    return service.revokeServiceKey(actor(user), id, keyId)
        ? ResponseEntity.noContent().build()
        : ResponseEntity.notFound().build();
  }

  private static OrganizationRole role(String raw) {
    try {
      return OrganizationRole.valueOf(raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException unknown) {
      throw new InvalidRequestException("Unknown organization role: " + raw);
    }
  }
}
