package com.krizaka.users.domain.ports.outbound;

import java.util.UUID;

/** Outbound port defining user interception persistence operations. */
public interface UserInterceptionRepositoryPort {

  /**
   * Triggers an active interception block for the user.
   *
   * @param userId The user UUID.
   * @param interceptionType The type of interception.
   * @param schemaId The associated configuration schema ID.
   */
  void triggerInterception(UUID userId, String interceptionType, String schemaId);

  /**
   * Deletes an active interception block for the user.
   *
   * @param userId The user UUID.
   * @param interceptionType The type of interception.
   */
  void deleteInterception(UUID userId, String interceptionType);
}
