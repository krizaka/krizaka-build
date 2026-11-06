package com.krizaka.users.application.service;

import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.model.PasswordResetRequestedEvent;
import com.krizaka.users.domain.model.PasswordResetToken;
import com.krizaka.users.domain.ports.inbound.PasswordPolicyService;
import com.krizaka.users.domain.ports.inbound.PasswordRecoveryService;
import com.krizaka.users.domain.ports.outbound.CryptographyPort;
import com.krizaka.users.domain.ports.outbound.PasswordEventPublisher;
import com.krizaka.users.domain.ports.outbound.PasswordResetTokenRepositoryPort;
import com.krizaka.users.domain.ports.outbound.UserRepositoryPort;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Package-private implementation of the password recovery lifecycle.
 *
 * <p>Handles token generation, SHA-256 hashing, expiration enforcement, and BCrypt password
 * updates. The {@code requestPasswordReset} method is intentionally non-revealing — it always
 * succeeds to prevent user enumeration attacks.
 */
@Service
class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

  private static final Logger logger = LoggerFactory.getLogger(PasswordRecoveryServiceImpl.class);
  private static final int TOKEN_EXPIRY_MINUTES = 15;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final PasswordResetTokenRepositoryPort tokenRepository;
  private final UserRepositoryPort userRepository;
  private final CryptographyPort cryptography;
  private final PasswordPolicyService passwordPolicy;
  private final PasswordEventPublisher passwordEventPublisher;

  PasswordRecoveryServiceImpl(
      PasswordResetTokenRepositoryPort tokenRepository,
      UserRepositoryPort userRepository,
      CryptographyPort cryptography,
      PasswordPolicyService passwordPolicy,
      PasswordEventPublisher passwordEventPublisher) {
    this.tokenRepository =
        Objects.requireNonNull(tokenRepository, "PasswordResetTokenRepositoryPort cannot be null");
    this.userRepository =
        Objects.requireNonNull(userRepository, "UserRepositoryPort cannot be null");
    this.cryptography = Objects.requireNonNull(cryptography, "CryptographyPort cannot be null");
    this.passwordPolicy =
        Objects.requireNonNull(passwordPolicy, "PasswordPolicyService cannot be null");
    this.passwordEventPublisher =
        Objects.requireNonNull(passwordEventPublisher, "PasswordEventPublisher cannot be null");
  }

  @Override
  public void requestPasswordReset(String email) {
    Objects.requireNonNull(email, "Email cannot be null");

    // Always succeed to prevent user enumeration — silently return if user does not exist
    boolean userExists = userRepository.findByEmail(email).isPresent();
    if (!userExists) {
      logger.debug("Password reset requested for non-existent email (suppressed): {}", email);
      return;
    }

    // Purge any existing tokens for this email (single-use guarantee)
    tokenRepository.deleteByEmail(email);

    // Generate cryptographically secure plaintext token
    byte[] randomBytes = new byte[32];
    SECURE_RANDOM.nextBytes(randomBytes);
    String plaintextToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    String tokenHash = cryptography.hashToken(plaintextToken);
    Instant expiresAt = Instant.now().plus(TOKEN_EXPIRY_MINUTES, ChronoUnit.MINUTES);

    PasswordResetToken resetToken =
        new PasswordResetToken(UUID.randomUUID().toString(), email, tokenHash, expiresAt);
    tokenRepository.save(resetToken);

    // Publish event for downstream email integration
    passwordEventPublisher.publish(new PasswordResetRequestedEvent(email, plaintextToken));

    // Log hash prefix for dev correlation — plaintext is NEVER logged (§1 Log Scrubbing)
    if (logger.isInfoEnabled()) {
      logger.info(
          "Password reset token generated for email={}, hash_prefix={}",
          email,
          tokenHash.substring(0, 8));
    }
  }

  @Override
  public void resetPassword(String token, String newPassword) {
    Objects.requireNonNull(token, "Token cannot be null");
    Objects.requireNonNull(newPassword, "New password cannot be null");
    String acceptedPassword = passwordPolicy.accept(newPassword);

    String tokenHash = cryptography.hashToken(token);

    PasswordResetToken resetToken =
        tokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidRequestException("Invalid or expired reset token"));

    if (resetToken.isExpired()) {
      tokenRepository.deleteById(resetToken.id());
      throw new InvalidRequestException("Invalid or expired reset token");
    }

    String encodedPassword = cryptography.encodePassword(acceptedPassword);
    userRepository.updatePasswordHashByEmail(resetToken.email(), encodedPassword);

    // Delete the token — single-use guarantee
    tokenRepository.deleteById(resetToken.id());

    logger.info("Password successfully reset for email={}", resetToken.email());
  }
}
