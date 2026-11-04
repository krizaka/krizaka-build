package com.krizaka.users.application.service;

import com.krizaka.users.domain.exception.CredentialGrantRefusedException;
import com.krizaka.users.domain.exception.CredentialNotFoundException;
import com.krizaka.users.domain.exception.InvalidRequestException;
import com.krizaka.users.domain.model.CredentialGrant;
import com.krizaka.users.domain.ports.inbound.CredentialGrantService;
import com.krizaka.users.domain.ports.inbound.IdentityService;
import com.krizaka.users.domain.ports.outbound.AdminUserRepositoryPort;
import com.krizaka.users.domain.ports.outbound.CredentialGrantRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link CredentialGrantService}: grants are 256-bit random secrets, stored as their
 * SHA-256, live for {@code krizaka.users.credential-grant.ttl} (1 to 60 minutes, 10 by default) and
 * are consumed by one conditional update, so two concurrent redemptions cannot both succeed.
 */
@Service
@Transactional
class CredentialGrantServiceImpl implements CredentialGrantService {

  // @fixed: grants are 32 random bytes (256 bits), the strength of the key they release.
  private static final int GRANT_BYTES = 32;
  private static final Duration MAX_TTL = Duration.ofHours(1);
  // @fixed: expired rows are kept a day for the audit trail's sake, then purged.
  private static final Duration KEEP_EXPIRED = Duration.ofDays(1);
  private static final Pattern RUN_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
  private static final int MAX_GRANT = 64;

  private final CredentialGrantRepositoryPort grants;
  private final IdentityService identityService;
  private final AdminUserRepositoryPort audit;
  private final Duration ttl;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  @Autowired
  CredentialGrantServiceImpl(
      CredentialGrantRepositoryPort grants,
      IdentityService identityService,
      AdminUserRepositoryPort audit,
      @Value("${krizaka.users.credential-grant.ttl:PT10M}") Duration ttl) {
    this(grants, identityService, audit, ttl, Clock.systemUTC());
  }

  CredentialGrantServiceImpl(
      CredentialGrantRepositoryPort grants,
      IdentityService identityService,
      AdminUserRepositoryPort audit,
      Duration ttl,
      Clock clock) {
    this.grants = Objects.requireNonNull(grants, "CredentialGrantRepositoryPort");
    this.identityService = Objects.requireNonNull(identityService, "IdentityService");
    this.audit = Objects.requireNonNull(audit, "AdminUserRepositoryPort");
    this.clock = Objects.requireNonNull(clock, "Clock");
    if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(MAX_TTL) > 0) {
      throw new IllegalArgumentException(
          "krizaka.users.credential-grant.ttl must be within (0, 1 h], was " + ttl);
    }
    this.ttl = ttl;
  }

  @Override
  public CredentialGrant issue(String userId, String providerName, String runId) {
    if (!isRunId(runId)) {
      throw new InvalidRequestException("runId must match " + RUN_ID.pattern());
    }
    boolean hasKey =
        identityService.getUserCredentials(userId).stream()
            .anyMatch(c -> c.providerName().equals(providerName) && c.configured());
    if (!hasKey) {
      throw new CredentialNotFoundException(providerName);
    }
    Instant now = clock.instant();
    grants.purgeExpiredBefore(now.minus(KEEP_EXPIRED));
    byte[] bytes = new byte[GRANT_BYTES];
    random.nextBytes(bytes);
    String grant = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expiresAt = now.plus(ttl);
    grants.save(hash(grant), userId, providerName, runId, expiresAt);
    audit.appendAudit(
        userId, "user", "credential.grant.issued", userId, detail(providerName, runId));
    return new CredentialGrant(grant, expiresAt);
  }

  @Override
  @Transactional(noRollbackFor = CredentialGrantRefusedException.class)
  public String redeem(String grant, String providerName, String runId, String caller) {
    boolean wellFormed = isRunId(runId) && grant != null && grant.length() <= MAX_GRANT;
    String userId =
        (wellFormed
                ? grants.consume(hash(grant), providerName, runId, clock.instant())
                : java.util.Optional.<String>empty())
            .orElseThrow(
                () -> {
                  audit.appendAuditOutcome(
                      null,
                      caller,
                      "credential.used",
                      null,
                      detail(providerName, wellFormed ? runId : "<malformed>"),
                      "refused");
                  return new CredentialGrantRefusedException();
                });
    String key =
        identityService
            .getDecryptedApiKey(userId, providerName)
            .orElseThrow(() -> new CredentialNotFoundException(providerName));
    audit.appendAudit(null, caller, "credential.used", userId, detail(providerName, runId));
    return key;
  }

  private static boolean isRunId(String runId) {
    return runId != null && RUN_ID.matcher(runId).matches();
  }

  private static String detail(String providerName, String runId) {
    return "provider=" + providerName + " run=" + runId;
  }

  private static String hash(String grant) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(grant.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
