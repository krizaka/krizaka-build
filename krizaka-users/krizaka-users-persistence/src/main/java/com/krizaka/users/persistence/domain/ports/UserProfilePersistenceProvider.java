package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.UserProfileDto;
import java.util.Optional;

/** Port interface for managing UserProfile persistence operations. */
public interface UserProfilePersistenceProvider {

  Optional<UserProfileDto> findByUserId(String userId);

  UserProfileDto save(UserProfileDto profileDto);
}
