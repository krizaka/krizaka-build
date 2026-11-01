package com.krizaka.users.application.service;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.LockoutPolicy;
import com.krizaka.users.domain.model.LockoutScope;
import com.krizaka.users.domain.model.LockoutState;
import com.krizaka.users.domain.ports.inbound.LoginLockoutService;
import com.krizaka.users.domain.ports.outbound.AdminUserRepositoryPort;
import com.krizaka.users.domain.ports.outbound.LockoutRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link LoginLockoutService} on the database. Subjects are stored as SHA-256 of the
 * lower-cased account (and address), so the table holds no mailbox; a changed case cannot buy a
 * fresh counter.
 */
@Service
@Transactional
class LoginLockoutServiceImpl implements LoginLockoutService {

  private final LockoutRepositoryPort store;
  private final AdminUserRepositoryPort audit;
  private final Clock clock;

  @Autowired
  LoginLockoutServiceImpl(LockoutRepositoryPort store, AdminUserRepositoryPort audit) {
    this(store, audit, Clock.systemUTC());
  }

  LoginLockoutServiceImpl(LockoutRepositoryPort store, AdminUserRepositoryPort audit, Clock clock) {
    this.store = Objects.requireNonNull(store, "LockoutRepositoryPort");
    this.audit = Objects.requireNonNull(audit, "AdminUserRepositoryPort");
    this.clock = Objects.requireNonNull(clock, "Clock");
  }

  @Override
  @Transactional(readOnly = true)
  public long retryAfterSeconds(String account, String ip) {
    var now = clock.instant();
    long pair =
        LockoutState.retryAfterSeconds(
            store.find(LockoutScope.PAIR, hash("p", account, ip)).orElse(null), now);
    long all =
        LockoutState.retryAfterSeconds(
            store.find(LockoutScope.ACCOUNT, hash("a", account, null)).orElse(null), now);
    return Math.max(pair, all);
  }

  @Override
  public void recordFailure(String account, String ip) {
    LockoutPolicy policy = store.policy();
    // Counters past their memory and their lock are dead weight: forget them as we go.
    store.purgeIdleBefore(clock.instant().minus(policy.forgetAfter().plus(policy.maxDelay())));
    bump(LockoutScope.PAIR, hash("p", account, ip), policy.threshold(), policy);
    bump(LockoutScope.ACCOUNT, hash("a", account, null), policy.accountThreshold(), policy);
  }

  @Override
  public void recordSuccess(String account, String ip) {
    store.delete(LockoutScope.PAIR, hash("p", account, ip));
  }

  @Override
  @Transactional(readOnly = true)
  public LockoutPolicy policy() {
    return store.policy();
  }

  @Override
  public LockoutPolicy updatePolicy(AdminActor actor, LockoutPolicy policy) {
    LockoutPolicy old = store.policy();
    store.savePolicy(policy, actor.label());
    audit.appendAudit(
        actor.id(),
        actor.label(),
        "identity.lockout_policy_changed",
        "lockout-policy",
        "from=" + describe(old) + " to=" + describe(policy));
    return policy;
  }

  private void bump(LockoutScope scope, String subject, int threshold, LockoutPolicy policy) {
    LockoutState old = store.findForUpdate(scope, subject).orElse(null);
    store.save(scope, subject, LockoutState.afterFailure(old, threshold, policy, clock.instant()));
  }

  private static String describe(LockoutPolicy p) {
    return p.threshold()
        + "/"
        + p.accountThreshold()
        + "/"
        + p.baseDelay()
        + "/"
        + p.maxDelay()
        + "/"
        + p.forgetAfter();
  }

  private static String hash(String kind, String account, String ip) {
    String subject =
        kind + "|" + account.strip().toLowerCase(Locale.ROOT) + (ip == null ? "" : "|" + ip);
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(subject.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
