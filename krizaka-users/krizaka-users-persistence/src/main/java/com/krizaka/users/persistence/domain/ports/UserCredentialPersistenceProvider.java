package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.UserCredentialDto;
import java.util.List;
import java.util.Optional;

/** Port interface for managing UserCredential persistence operations. */
public interface UserCredentialPersistenceProvider {

  List<UserCredentialDto> findByUserId(String userId);

  Optional<UserCredentialDto> findByUserIdAndProviderName(String userId, String providerName);

  UserCredentialDto save(UserCredentialDto credentialDto);

  void deleteByUserIdAndProviderName(String userId, String providerName);
}
