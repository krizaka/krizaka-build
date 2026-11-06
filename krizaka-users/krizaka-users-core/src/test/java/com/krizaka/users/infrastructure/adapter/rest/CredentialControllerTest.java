package com.krizaka.users.infrastructure.adapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.krizaka.users.domain.exception.CredentialConflictException;
import com.krizaka.users.domain.exception.CredentialNotFoundException;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.model.UserCredential;
import com.krizaka.users.domain.ports.inbound.IdentityService;
import com.krizaka.users.infrastructure.adapter.rest.dto.RotateCredentialRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.UserCredentialRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/**
 * krizaka-internal#2359 slice 1a: the full CRUD of a user's provider key, write-only — no response
 * body, no error body and no log line carries the key; a missing (or another user's) key is 404, a
 * second one 409.
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CredentialControllerTest {

  private static final String KEY = "not-a-real-provider-key";
  private static final String ROTATED = "not-a-real-rotated-key";

  @Mock private IdentityService identityService;

  private final JsonMapper json = JsonMapper.builder().build();
  private final IdentityErrorResolver errors = new IdentityErrorResolver();
  private CredentialController controller;
  private User user;
  private String userId;

  @BeforeEach
  void setUp() {
    controller = new CredentialController(identityService);
    user =
        new User(
            UUID.randomUUID(),
            "krizaka",
            "krizaka@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of());
    userId = user.id().toString();
  }

  private String body(Object value) {
    return json.writeValueAsString(value);
  }

  @Test
  void createReadRotateDeleteNeverAnswerTheKey(CapturedOutput output) {
    when(identityService.getUserCredentials(userId))
        .thenReturn(List.of(new UserCredential("runway", true)));

    var created = controller.createCredential(new UserCredentialRequest("runway", KEY), user);
    var listed = controller.getCredentials(user);
    var rotated = controller.rotateCredential("runway", new RotateCredentialRequest(ROTATED), user);
    var deleted = controller.deleteCredential("runway", user);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(rotated.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(identityService).createUserCredential(userId, "runway", KEY);
    verify(identityService).rotateUserCredential(userId, "runway", ROTATED);
    verify(identityService).deleteUserCredential(userId, "runway");
    for (Object answer : List.of(created, listed, rotated, deleted)) {
      assertThat(body(((org.springframework.http.ResponseEntity<?>) answer).getBody()))
          .doesNotContain(KEY)
          .doesNotContain(ROTATED);
    }
    assertThat(body(listed.getBody())).contains("runway");
    assertThat(output.getAll()).doesNotContain(KEY).doesNotContain(ROTATED);
  }

  @Test
  void anotherUsersOrAMissingKeyIs404AndASecondOne409WithoutTheKey(CapturedOutput output) {
    doThrow(new CredentialNotFoundException("runway"))
        .when(identityService)
        .deleteUserCredential(userId, "runway");
    doThrow(new CredentialConflictException("runway"))
        .when(identityService)
        .createUserCredential(userId, "runway", KEY);

    HttpStatus missing;
    String missingBody;
    try {
      controller.deleteCredential("runway", user);
      throw new AssertionError("expected a 404");
    } catch (CredentialNotFoundException e) {
      var answer = errors.handleCredentialNotFound(e);
      missing = HttpStatus.valueOf(answer.getStatusCode().value());
      missingBody = body(answer.getBody());
    }
    HttpStatus conflict;
    String conflictBody;
    try {
      controller.createCredential(new UserCredentialRequest("runway", KEY), user);
      throw new AssertionError("expected a 409");
    } catch (CredentialConflictException e) {
      var answer = errors.handleCredentialConflict(e);
      conflict = HttpStatus.valueOf(answer.getStatusCode().value());
      conflictBody = body(answer.getBody());
    }

    assertThat(missing).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(conflict).isEqualTo(HttpStatus.CONFLICT);
    assertThat(missingBody + conflictBody + output.getAll()).doesNotContain(KEY);
  }

  @Test
  void theRequestsNeverPrintTheKey() {
    assertThat(new UserCredentialRequest("runway", KEY).toString()).doesNotContain(KEY);
    assertThat(new RotateCredentialRequest(ROTATED).toString()).doesNotContain(ROTATED);
  }
}
