package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.model.UserCredential;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface defining the identity lifecycle contract consumed by the router adapter layer.
 *
 * <p>The router binds exclusively to this interface via constructor injection. The concrete
 * implementation ({@code IdentityServiceImpl}) is package-private within {@code krizaka-users-core}
 * and discovered by Spring component scanning.
 *
 * <p>Architectural invariants:
 *
 * <ul>
 *   <li>Router has zero compile-time awareness of the implementation class [ERR-102]
 *   <li>All methods return domain records, never entities [§2.A Model Separation]
 *   <li>Authentication failures throw explicit exceptions, never return null [ERR-106]
 * </ul>
 *
 * @see User
 */
public interface IdentityService {

  /**
   * Resolves a complete {@link User} profile by their unique identifier.
   *
   * @param userId The UUID string identifying the user.
   * @return The resolved {@link User} record, or {@code null} if no user exists.
   */
  User getUser(String userId);

  /**
   * Authenticates a user by email and password, filtering out disabled accounts.
   *
   * @param email Plaintext email.
   * @param password Plaintext password.
   * @return Fully resolved {@link User} if successful.
   * @throws com.krizaka.users.domain.exception.BadCredentialsException if authentication fails.
   */
  User authenticate(String email, String password);

  /**
   * Registers a new user account, initiating verification or interceptions if configured.
   *
   * @param username Display name.
   * @param email Email address (must be unique).
   * @param password Plaintext password to hash.
   * @param language Initial language preference.
   * @return Fully resolved {@link User}.
   * @throws com.krizaka.users.domain.exception.UserAlreadyExistsException if email is taken.
   */
  User register(String username, String email, String password, String language);

  /**
   * Merges preferences map into user preferences.
   *
   * @param userId The user UUID string.
   * @param preferences The preference overrides to merge.
   * @return Fully resolved updated {@link User} record.
   */
  User updatePreferences(String userId, Map<String, Object> preferences);

  /**
   * Resolves an active interception by merging replies and deleting the block row.
   *
   * @param userId The user ID.
   * @param interceptionType The type of interception.
   * @param schemaId The configuration schema ID.
   * @param responses Map of user inputs to merge.
   */
  void resolveInterception(
      UUID userId, String interceptionType, String schemaId, Map<String, Object> responses);

  /**
   * Verifies a verification token and enables the target user account.
   *
   * @param token Plaintext token.
   * @return True if verified successfully; false otherwise.
   */
  boolean verifyToken(String token);

  /**
   * Returns whether email verification is required for new registrations.
   *
   * <p>Encapsulates the {@code emailVerification.enabled} config check so that router controllers
   * remain completely oblivious to {@code IdentityInfrastructureProperties} (ERR-113).
   *
   * @return {@code true} if a verification email must be sent before account activation.
   */
  boolean requiresEmailVerification();

  /**
   * Loads a raw interception schema JSON string by its schema identifier.
   *
   * <p>Encapsulates the {@code interceptions.schemas} resource resolution so that router
   * controllers remain completely oblivious to {@code IdentityInfrastructureProperties} (ERR-113).
   *
   * @param schemaId The schema identifier key (e.g., {@code "onboarding"}).
   * @return The raw JSON string content, or {@code null} if not found or unconfigured.
   */
  String loadInterceptionSchema(String schemaId);

  /** Retrieves configured credentials for a user. */
  List<UserCredential> getUserCredentials(String userId);

  /** Saves or rotates a user API key. */
  void saveUserCredential(String userId, String providerName, String apiKey);

  /** Deletes a user API key. */
  void deleteUserCredential(String userId, String providerName);

  /** Resolves the decrypted API key for a user and provider. */
  Optional<String> getDecryptedApiKey(String userId, String providerName);
}
