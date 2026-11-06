package com.krizaka.users.application.service;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.EnvironmentRegistration;
import com.krizaka.users.domain.model.SocialConnection;
import com.krizaka.users.domain.model.SocialConnectionView;
import com.krizaka.users.domain.ports.inbound.SocialConnectionService;
import com.krizaka.users.domain.ports.outbound.AdminUserRepositoryPort;
import com.krizaka.users.domain.ports.outbound.SecretSealPort;
import com.krizaka.users.domain.ports.outbound.SocialConnectionRepositoryPort;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link SocialConnectionService}: secrets are sealed before they are stored, opened
 * only in {@link #clientSecret}, never logged, and every change leaves an audit line.
 */
@Service
@Transactional
class SocialConnectionServiceImpl implements SocialConnectionService {

  private static final Logger LOG = LoggerFactory.getLogger(SocialConnectionServiceImpl.class);
  private static final String ENV = "env";

  private final SocialConnectionRepositoryPort connections;
  private final SecretSealPort vault;
  private final AdminUserRepositoryPort audit;

  SocialConnectionServiceImpl(
      SocialConnectionRepositoryPort connections,
      SecretSealPort vault,
      AdminUserRepositoryPort audit) {
    this.connections = Objects.requireNonNull(connections, "SocialConnectionRepositoryPort");
    this.vault = Objects.requireNonNull(vault, "SecretSealPort");
    this.audit = Objects.requireNonNull(audit, "AdminUserRepositoryPort");
  }

  @Override
  @Transactional(readOnly = true)
  public List<SocialConnectionView> list() {
    return connections.findAll().stream().map(SocialConnectionView::of).toList();
  }

  @Override
  public int importFromEnvironment(AdminActor actor, List<EnvironmentRegistration> registrations) {
    List<EnvironmentRegistration> todo =
        registrations.stream()
            .filter(EnvironmentRegistration::configured)
            .filter(r -> connections.find(r.provider()).isEmpty())
            .toList();
    if (todo.isEmpty()) {
      return 0;
    }
    if (!vault.available()) {
      LOG.warn(
          "AUTH_VAULT_KEY is not set: {} identity provider(s) from the environment are not imported",
          todo.size());
      return 0;
    }
    for (EnvironmentRegistration r : todo) {
      connections.save(
          new SocialConnection(
              r.provider(),
              r.clientId(),
              vault.seal(r.clientSecret()),
              vault.currentKeyId(),
              ENV,
              null));
      audit.appendAudit(
          actor.id(),
          actor.label(),
          "identity.social_connection_imported",
          r.provider(),
          "source=env keyId=" + vault.currentKeyId());
    }
    return todo.size();
  }

  @Override
  public int rewrapAll(AdminActor actor) {
    int done = 0;
    for (SocialConnection c : connections.findAll()) {
      if (c.sealedClientSecret() == null
          || vault.currentKeyId().equals(vault.keyIdOf(c.sealedClientSecret()))) {
        continue;
      }
      connections.save(
          new SocialConnection(
              c.provider(),
              c.clientId(),
              vault.rewrap(c.sealedClientSecret()),
              vault.currentKeyId(),
              c.source(),
              null));
      audit.appendAudit(
          actor.id(),
          actor.label(),
          "identity.social_connection_rewrapped",
          c.provider(),
          "from=" + c.keyId() + " to=" + vault.currentKeyId());
      done++;
    }
    return done;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<String> clientSecret(String provider) {
    return connections.find(provider).map(SocialConnection::sealedClientSecret).map(vault::open);
  }
}
