package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.UserProfile;

/** Boundary provider interface for retrieving user profile data. */
public interface UserProfileProvider {

  /**
   * Fetches the onboarding profile for a given user ID.
   *
   * @param userId The unique user identifier.
   * @return The immutable {@link UserProfile} domain DTO, or null if not found.
   */
  UserProfile getProfile(String userId);
}
