package com.krizaka.users.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.krizaka.users.domain.exception.RegistrationClosedException;
import com.krizaka.users.domain.model.ExtractedProfile;
import com.krizaka.users.domain.model.Persona;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.outbound.AuthorityRepositoryPort;
import com.krizaka.users.domain.ports.outbound.OAuth2ProviderVerifier;
import com.krizaka.users.domain.ports.outbound.UserRepositoryPort;
import com.krizaka.users.infrastructure.config.IdentityInfrastructureProperties;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

/** Unit tests for {@link IdentityReconciliationServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class IdentityReconciliationServiceImplTest {

  @Mock private OAuth2ProviderVerifier googleVerifier;
  @Mock private UserRepositoryPort userRepository;
  @Mock private AuthorityRepositoryPort authorityRepository;
  @Mock private PlatformTransactionManager transactionManager;
  @Mock private TransactionStatus transactionStatus;

  private static final IdentityInfrastructureProperties OPEN =
      new IdentityInfrastructureProperties(null, null, null, null);
  private static final IdentityInfrastructureProperties CLOSED =
      new IdentityInfrastructureProperties(
          null, null, null, new IdentityInfrastructureProperties.Registration(false));

  private IdentityReconciliationServiceImpl service;

  private IdentityReconciliationServiceImpl closedService() {
    return new IdentityReconciliationServiceImpl(
        List.of(googleVerifier), userRepository, authorityRepository, CLOSED, transactionManager);
  }

  @BeforeEach
  void setUp() {
    lenient().when(googleVerifier.supports("google")).thenReturn(true);
    service =
        new IdentityReconciliationServiceImpl(
            List.of(googleVerifier), userRepository, authorityRepository, OPEN, transactionManager);
  }

  @Test
  void reconcile_unsupportedProvider_throwsException() {
    IdentityReconciliationServiceImpl emptyService =
        new IdentityReconciliationServiceImpl(
            List.of(), userRepository, authorityRepository, OPEN, transactionManager);

    assertThrows(IllegalArgumentException.class, () -> emptyService.reconcile("google", "token"));
  }

  @Test
  void constructor_nullVerifiers_initializesEmpty() {
    IdentityReconciliationServiceImpl nullService =
        new IdentityReconciliationServiceImpl(
            null, userRepository, authorityRepository, OPEN, transactionManager);

    assertThrows(IllegalArgumentException.class, () -> nullService.reconcile("google", "token"));
  }

  @Test
  void constructor_withVerifiers_logsSizeInfo() {
    assertDoesNotThrow(
        () ->
            new IdentityReconciliationServiceImpl(
                List.of(googleVerifier),
                userRepository,
                authorityRepository,
                OPEN,
                transactionManager));
  }

  @Test
  void reconcile_existingUser_returnsExistingUser() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    User existingUser = Persona.freeUser();

    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.of(existingUser));

    User result = service.reconcile("google", "id-token");

    assertEquals(existingUser, result);
    verify(userRepository, never()).createFederated(any(), any(), any());
    verify(authorityRepository, never()).saveAuthority(any(), any());
  }

  @Test
  void reconcile_newUser_provisionsAndReturnsSavedUser() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    User savedUser = Persona.freeUser();

    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty());
    when(userRepository.createFederated(any(User.class), eq("google"), eq("google-id")))
        .thenReturn(savedUser);

    User result = service.reconcile("google", "id-token");

    assertEquals(savedUser, result);
    verify(userRepository).createFederated(any(User.class), eq("google"), eq("google-id"));
    verify(authorityRepository).saveAuthority(anyString(), eq("ROLE_USER"));
  }

  @Test
  void reconcile_newUserConcurrentCreation_fallsBackToLookup() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    User existingUser = Persona.freeUser();

    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty()) // first lookup: not found
        .thenReturn(Optional.of(existingUser)); // fallback lookup: found
    when(userRepository.createFederated(any(User.class), eq("google"), eq("google-id")))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    User result = service.reconcile("google", "id-token");

    assertEquals(existingUser, result);
    verify(authorityRepository, never()).saveAuthority(any(), any());
  }

  @Test
  void reconcile_newUserConcurrentCreationFallbackFails_throwsException() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);

    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty());
    when(userRepository.createFederated(any(User.class), eq("google"), eq("google-id")))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThrows(IllegalStateException.class, () -> service.reconcile("google", "id-token"));
  }

  @Test
  void reconcile_registrationClosed_unknownIdentity_refusesAndCreatesNothing() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty());

    IdentityReconciliationServiceImpl closed = closedService();
    assertThrows(RegistrationClosedException.class, () -> closed.reconcile("google", "id-token"));

    verify(userRepository, never()).createFederated(any(), any(), any());
    verify(authorityRepository, never()).saveAuthority(any(), any());
  }

  @Test
  void reconcile_registrationClosed_existingIdentity_stillSignsIn() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    User existingUser = Persona.freeUser();
    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.of(existingUser));

    assertEquals(existingUser, closedService().reconcile("google", "id-token"));
  }

  // A federated user must be stored WITH its provider identity, or the next sign-in of the same
  // person finds nobody and fails on the unique username.
  @Test
  void reconcile_newUser_isStoredWithItsProviderAndProviderId() {
    ExtractedProfile profile =
        new ExtractedProfile("test@example.com", "google-id", "Test User", null);
    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty());
    when(userRepository.createFederated(any(User.class), eq("google"), eq("google-id")))
        .thenReturn(Persona.freeUser());

    service.reconcile("google", "id-token");

    verify(userRepository).createFederated(any(User.class), eq("google"), eq("google-id"));
    verify(userRepository, never()).create(any(), any());
  }

  // Two people with the same display name can both sign in.
  @Test
  void reconcile_newUser_takesAFreeUsernameWhenTheDisplayNameIsTaken() {
    ExtractedProfile profile =
        new ExtractedProfile("alex@example.com", "google-id", "Alex Martin", null);
    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "google-id"))
        .thenReturn(Optional.empty());
    when(userRepository.usernameTaken("Alex Martin")).thenReturn(true);
    when(userRepository.usernameTaken(argThat(u -> !"Alex Martin".equals(u)))).thenReturn(false);
    ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
    when(userRepository.createFederated(created.capture(), eq("google"), eq("google-id")))
        .thenReturn(Persona.freeUser());

    service.reconcile("google", "id-token");

    assertTrue(created.getValue().username().startsWith("Alex Martin-"));
    assertTrue(created.getValue().username().length() <= 100);
  }

  // A provider that gives no name must not turn the e-mail address into a public username.
  @Test
  void reconcile_newUserWithoutAName_neverUsesTheEmailAsUsername() {
    ExtractedProfile profile =
        new ExtractedProfile("private.person@example.com", "gh-1", null, null);
    when(googleVerifier.verifyAndExtract("id-token")).thenReturn(profile);
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    when(userRepository.findByProviderAndProviderId("google", "gh-1")).thenReturn(Optional.empty());
    ArgumentCaptor<User> created = ArgumentCaptor.forClass(User.class);
    when(userRepository.createFederated(created.capture(), eq("google"), eq("gh-1")))
        .thenReturn(Persona.freeUser());

    service.reconcile("google", "id-token");

    assertEquals("private.person", created.getValue().username());
    assertEquals("private.person@example.com", created.getValue().email());
  }
}
