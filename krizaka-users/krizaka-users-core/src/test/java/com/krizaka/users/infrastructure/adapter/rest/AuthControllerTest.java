package com.krizaka.users.infrastructure.adapter.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.exception.BadCredentialsException;
import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.IdentityReconciliationService;
import com.krizaka.users.domain.ports.inbound.IdentityService;
import com.krizaka.users.domain.ports.inbound.PasswordRecoveryService;
import com.krizaka.users.infrastructure.adapter.rest.dto.AuthResponse;
import com.krizaka.users.infrastructure.adapter.rest.dto.ForgotPasswordRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.GenericMessageResponse;
import com.krizaka.users.infrastructure.adapter.rest.dto.LoginRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.OAuthRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.RegisterRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.ResetPasswordRequest;
import com.krizaka.users.infrastructure.adapter.rest.dto.VerifyTokenRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock private IdentityService identityService;

  @Mock private IdentityReconciliationService reconciliationService;

  @Mock private PasswordRecoveryService passwordRecoveryService;

  @Mock private com.krizaka.users.domain.ports.inbound.TokenService tokenService;

  private AuthController controller;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient()
        .when(tokenService.issue(org.mockito.ArgumentMatchers.any()))
        .thenReturn("issued-jwt");
    controller =
        new AuthController(
            identityService, reconciliationService, passwordRecoveryService, tokenService);
  }

  @Test
  void login_validCredentials_returnsOk() {
    LoginRequest request = new LoginRequest("test@example.com", "password");
    User user =
        new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());
    when(identityService.authenticate("test@example.com", "password")).thenReturn(user);

    ResponseEntity<Object> response = controller.login(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody() instanceof AuthResponse);
    AuthResponse body = (AuthResponse) response.getBody();
    assertEquals("testuser", body.username());
  }

  @Test
  void login_invalidCredentials_returnsUnauthorized() {
    LoginRequest request = new LoginRequest("test@example.com", "password");
    when(identityService.authenticate("test@example.com", "password"))
        .thenThrow(new BadCredentialsException("Invalid"));

    ResponseEntity<Object> response = controller.login(request);

    assertNotNull(response);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    assertTrue(response.getBody() instanceof Map);
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals("Invalid email or password", body.get("error"));
  }

  @Test
  void oauthLogin_validToken_returnsOk() {
    OAuthRequest request =
        new OAuthRequest("google", "google-token", "oauth@example.com", "oauthuser");
    User user =
        new User(
            UUID.randomUUID(),
            "oauthuser",
            "oauth@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());
    when(reconciliationService.reconcile("google", "google-token")).thenReturn(user);

    ResponseEntity<Object> response = controller.oauthLogin(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertTrue(response.getBody() instanceof AuthResponse);
  }

  @Test
  void oauthLogin_invalidToken_returnsUnauthorized() {
    OAuthRequest request =
        new OAuthRequest("google", "google-token", "oauth@example.com", "oauthuser");
    when(reconciliationService.reconcile("google", "google-token"))
        .thenThrow(new IllegalArgumentException("Invalid token"));

    ResponseEntity<Object> response = controller.oauthLogin(request);

    assertNotNull(response);
    assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
  }

  @Test
  void register_successfulWithoutVerification_returnsCreated() {
    RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password", "en");
    User user =
        new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());

    when(identityService.register("testuser", "test@example.com", "password", "en"))
        .thenReturn(user);
    when(identityService.requiresEmailVerification()).thenReturn(false);

    ResponseEntity<Object> response = controller.register(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody() instanceof AuthResponse);
  }

  @Test
  void register_successfulWithVerification_returnsCreatedAndVerificationFlag() {
    RegisterRequest request = new RegisterRequest("testuser", "test@example.com", "password", "en");
    User user =
        new User(
            UUID.randomUUID(),
            "testuser",
            "test@example.com",
            true,
            Set.of("ROLE_USER"),
            Map.of(),
            List.of());

    when(identityService.register("testuser", "test@example.com", "password", "en"))
        .thenReturn(user);
    when(identityService.requiresEmailVerification()).thenReturn(true);

    ResponseEntity<Object> response = controller.register(request);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertTrue(response.getBody() instanceof Map);
    Map<?, ?> body = (Map<?, ?>) response.getBody();
    assertEquals(true, body.get("requires_verification"));
  }

  @Test
  void verifyAccount_successful_returnsOk() {
    VerifyTokenRequest request = new VerifyTokenRequest("valid-token");
    when(identityService.verifyToken("valid-token")).thenReturn(true);

    ResponseEntity<Void> response = controller.verifyAccount(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void verifyAccount_failed_returnsBadRequest() {
    VerifyTokenRequest request = new VerifyTokenRequest("invalid-token");
    when(identityService.verifyToken("invalid-token")).thenReturn(false);

    ResponseEntity<Void> response = controller.verifyAccount(request);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }

  @Test
  void forgotPassword_always_returnsOk() {
    ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

    ResponseEntity<GenericMessageResponse> response = controller.forgotPassword(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(passwordRecoveryService).requestPasswordReset("test@example.com");
  }

  @Test
  void resetPassword_successful_returnsOk() {
    ResetPasswordRequest request = new ResetPasswordRequest("token", "newpassword");

    ResponseEntity<Object> response = controller.resetPassword(request);

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    verify(passwordRecoveryService).resetPassword("token", "newpassword");
  }

  @Test
  void resetPassword_invalidToken_returnsBadRequest() {
    ResetPasswordRequest request = new ResetPasswordRequest("token", "newpassword");
    doThrow(new InvalidRequestException("Invalid token"))
        .when(passwordRecoveryService)
        .resetPassword("token", "newpassword");

    ResponseEntity<Object> response = controller.resetPassword(request);

    assertNotNull(response);
    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
  }
}
