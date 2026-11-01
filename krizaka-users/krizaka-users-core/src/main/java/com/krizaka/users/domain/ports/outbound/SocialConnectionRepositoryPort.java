package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.SocialConnection;
import java.util.List;
import java.util.Optional;

/** Outbound port of the identity-provider connections (secrets sealed). */
public interface SocialConnectionRepositoryPort {

  List<SocialConnection> findAll();

  Optional<SocialConnection> find(String provider);

  void save(SocialConnection connection);
}
