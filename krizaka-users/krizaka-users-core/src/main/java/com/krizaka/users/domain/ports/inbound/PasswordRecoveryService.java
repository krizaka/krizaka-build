package com.krizaka.users.domain.ports.inbound;

/**
 * Port interface defining the password recovery lifecycle contract.
 *
 * <p>Separated from {@link IdentityService} to respect the Single Responsibility Principle. The
 * router binds to this interface via constructor injection, and the concrete implementation is
 * package-private within {@code krizaka-users-core}.
 *
 * <p>Architectural invariants:
 *
 * <ul>
 *   <li>The {@code requestPasswordReset} method MUST always succeed — no user enumeration [§7.1]
 *   <li>The {@code resetPassword} method throws explicit exceptions on failure [ERR-106]
 *   <li>Tokens are single-use and auto-expire after 15 minutes
 * </ul>
 */
public interface PasswordRecoveryService {

  /**
   * Initiates a password reset request for the given email.
   *
   * <p>Generates a cryptographically secure token, hashes it with SHA-256, and persists it with a
   * 15-minute expiration window. Always succeeds regardless of whether the email exists to prevent
   * account enumeration attacks.
   *
   * @param email The email address to initiate the reset for.
   */
  void requestPasswordReset(String email);

  /**
   * Executes a password reset using the plaintext token and new password.
   *
   * <p>Validates the token hash exists and has not expired, updates the user's password using
   * secure BCrypt hashing, and immediately deletes the token record to guarantee single-use
   * utility.
   *
   * @param token The plaintext reset token (received by the user).
   * @param newPassword The new password to set.
   * @throws com.krizaka.users.domain.exception.InvalidRequestException if the token is invalid,
   *     expired, or tampered.
   */
  void resetPassword(String token, String newPassword);
}
