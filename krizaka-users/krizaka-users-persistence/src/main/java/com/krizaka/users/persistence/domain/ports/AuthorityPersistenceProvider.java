package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.AuthorityDto;
import java.util.List;

/** Port interface for managing Authority persistence operations. */
public interface AuthorityPersistenceProvider {

  void saveAuthority(String userId, String authorityName);

  List<AuthorityDto> findByUserId(String userId);
}
