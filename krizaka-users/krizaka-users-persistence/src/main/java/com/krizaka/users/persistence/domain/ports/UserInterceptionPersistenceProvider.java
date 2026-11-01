package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.UserInterceptionDto;
import java.util.Optional;

/** Port interface for managing UserInterception persistence operations. */
public interface UserInterceptionPersistenceProvider {

  UserInterceptionDto save(UserInterceptionDto interceptionDto, String schemaId);

  void deleteByUserIdAndInterceptionType(String userId, String interceptionType);

  Optional<UserInterceptionDto> findByUserIdAndInterceptionType(
      String userId, String interceptionType);
}
