package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.VerificationTokenDto;
import java.util.Optional;

/** Port interface for managing VerificationToken persistence operations. */
public interface VerificationTokenPersistenceProvider {

  VerificationTokenDto save(VerificationTokenDto tokenDto);

  Optional<VerificationTokenDto> findByTokenHashAndUsedFalse(String tokenHash);

  Optional<VerificationTokenDto> findById(String tokenId);

  void deleteById(String tokenId);
}
