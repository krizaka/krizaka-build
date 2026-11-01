package com.krizaka.users.infrastructure.adapter.persistence;

import com.krizaka.users.domain.model.SocialConnection;
import com.krizaka.users.domain.ports.outbound.SocialConnectionRepositoryPort;
import com.krizaka.users.persistence.domain.model.SocialConnectionRow;
import com.krizaka.users.persistence.domain.ports.SocialConnectionPersistenceProvider;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Outbound adapter of the connections: maps the persistence rows to the domain records. */
@Component
class SocialConnectionRepositoryAdapter implements SocialConnectionRepositoryPort {

  private final SocialConnectionPersistenceProvider provider;

  SocialConnectionRepositoryAdapter(SocialConnectionPersistenceProvider provider) {
    this.provider = Objects.requireNonNull(provider, "SocialConnectionPersistenceProvider");
  }

  private static SocialConnection domain(SocialConnectionRow r) {
    return new SocialConnection(
        r.provider(), r.clientId(), r.sealedClientSecret(), r.keyId(), r.source(), r.updatedAt());
  }

  @Override
  public List<SocialConnection> findAll() {
    return provider.findAll().stream().map(SocialConnectionRepositoryAdapter::domain).toList();
  }

  @Override
  public Optional<SocialConnection> find(String id) {
    return provider.find(id).map(SocialConnectionRepositoryAdapter::domain);
  }

  @Override
  public void save(SocialConnection c) {
    provider.save(
        new SocialConnectionRow(
            c.provider(),
            c.clientId(),
            c.sealedClientSecret(),
            c.keyId(),
            c.source(),
            c.updatedAt()));
  }
}
