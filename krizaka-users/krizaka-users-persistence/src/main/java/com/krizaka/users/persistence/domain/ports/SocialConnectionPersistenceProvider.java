package com.krizaka.users.persistence.domain.ports;

import com.krizaka.users.persistence.domain.model.SocialConnectionRow;
import java.util.List;
import java.util.Optional;

/** Persistence port of the identity-provider connections (secrets already sealed). */
public interface SocialConnectionPersistenceProvider {

  List<SocialConnectionRow> findAll();

  Optional<SocialConnectionRow> find(String provider);

  /** Inserts or replaces the connection of {@code row.provider()}. */
  void save(SocialConnectionRow row);
}
