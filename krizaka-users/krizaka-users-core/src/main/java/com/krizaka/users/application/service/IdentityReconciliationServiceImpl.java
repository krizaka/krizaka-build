package com.krizaka.users.application.service;

import com.krizaka.users.domain.exception.RegistrationClosedException;
import com.krizaka.users.domain.model.ExtractedProfile;
import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.IdentityReconciliationService;
import com.krizaka.users.domain.ports.outbound.AuthorityRepositoryPort;
import com.krizaka.users.domain.ports.outbound.OAuth2ProviderVerifier;
import com.krizaka.users.domain.ports.outbound.UserRepositoryPort;
import com.krizaka.users.infrastructure.config.IdentityInfrastructureProperties;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Package-private concrete implementation of the {@link IdentityReconciliationService} port
 * interface.
 */
@Service
class IdentityReconciliationServiceImpl implements IdentityReconciliationService {

  private static final Logger logger =
      LoggerFactory.getLogger(IdentityReconciliationServiceImpl.class);

  // @fixed: the users.username column is VARCHAR(100); the base leaves room for the suffix.
  private static final int USERNAME_BASE_MAX = 80;
  // @fixed: tries before a UUID suffix; a technical bound, not a business setting.
  private static final int USERNAME_ATTEMPTS = 5;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final List<OAuth2ProviderVerifier> verifiers;
  private final UserRepositoryPort userRepository;
  private final AuthorityRepositoryPort authorityRepository;
  private final IdentityInfrastructureProperties properties;
  private final TransactionTemplate transactionTemplate;

  IdentityReconciliationServiceImpl(
      List<OAuth2ProviderVerifier> verifiers,
      UserRepositoryPort userRepository,
      AuthorityRepositoryPort authorityRepository,
      IdentityInfrastructureProperties properties,
      PlatformTransactionManager transactionManager) {
    this.verifiers = verifiers != null ? List.copyOf(verifiers) : List.of();
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
    this.properties = properties;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    logger.info(
        "Identity reconciliation service initialized with {} active provider verifier(s)",
        this.verifiers.size());
  }

  @Override
  public User reconcile(String provider, String idToken) {
    OAuth2ProviderVerifier verifier = resolveVerifier(provider);
    ExtractedProfile profile = verifier.verifyAndExtract(idToken);

    logger.debug("Token verified for provider={}, extractedEmail={}", provider, profile.email());

    return transactionTemplate.execute(
        status -> {
          Optional<User> existingUser =
              userRepository.findByProviderAndProviderId(provider, profile.providerId());

          if (existingUser.isPresent()) {
            logger.debug(
                "Existing federated user found for provider={}, providerId={}",
                provider,
                profile.providerId());
            return existingUser.get();
          }

          if (!properties.registration().enabled()) {
            logger.warn(
                "Federated sign-in refused for an unknown identity (registration closed):"
                    + " provider={}",
                provider);
            throw new RegistrationClosedException();
          }
          return provisionFederatedUser(provider, profile);
        });
  }

  private User provisionFederatedUser(String provider, ExtractedProfile profile) {
    String userId = UUID.randomUUID().toString();

    User newUser =
        new User(
            UUID.fromString(userId),
            freeUsername(displayName(profile)),
            profile.email(),
            true,
            Set.of("ROLE_USER"),
            Map.of("language", "en"),
            List.of(),
            "free");

    User savedUser;
    try {
      // The provider identity is stored with the user: the next sign-in finds this account by it.
      savedUser = userRepository.createFederated(newUser, provider, profile.providerId());
    } catch (DataIntegrityViolationException ex) {
      logger.warn(
          "Concurrent federated user creation detected for provider={}, providerId={}. "
              + "Falling back to lookup.",
          provider,
          profile.providerId());
      return userRepository
          .findByProviderAndProviderId(provider, profile.providerId())
          .orElseThrow(
              () ->
                  new IllegalStateException(
                      "Failed to provision or resolve federated user for provider=" + provider));
    }

    authorityRepository.saveAuthority(userId, "ROLE_USER");

    logger.info("Provisioned new federated user: provider={}, id={}", provider, userId);

    return savedUser;
  }

  /**
   * The name a person is shown under: the provider's display name, or — when the provider gives
   * none and the profile fell back to the e-mail address — the address's local part, so that an
   * e-mail address never becomes a public username.
   */
  private static String displayName(ExtractedProfile profile) {
    String name = profile.name() == null ? "" : profile.name().strip().replaceAll("\\s+", " ");
    if (name.isEmpty() || name.equalsIgnoreCase(profile.email())) {
      String email = profile.email();
      int at = email.indexOf('@');
      name = at > 0 ? email.substring(0, at) : "member";
    }
    return name.length() > USERNAME_BASE_MAX ? name.substring(0, USERNAME_BASE_MAX).strip() : name;
  }

  /**
   * The display name when it is free, else the name with a short random suffix ("Alex
   * Martin-7f3a").
   */
  private String freeUsername(String base) {
    if (!userRepository.usernameTaken(base)) {
      return base;
    }
    for (int attempt = 0; attempt < USERNAME_ATTEMPTS; attempt++) {
      String candidate = base + "-" + HexFormat.of().toHexDigits((short) RANDOM.nextInt(1 << 16));
      if (!userRepository.usernameTaken(candidate)) {
        return candidate;
      }
    }
    return base + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  private OAuth2ProviderVerifier resolveVerifier(String provider) {
    return verifiers.stream()
        .filter(v -> v.supports(provider))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No active verifier found for provider: "
                        + provider
                        + ". Ensure the provider is enabled in configuration."));
  }
}
