package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.RateLimitInfo;
import java.util.Optional;

/** Inbound port contract for resolving rate limit configs inside the identity domain. */
public interface RateLimitProvider {

  /**
   * Resolves the rate limit configuration details for a given tier key.
   *
   * @param tierKey The target tier ID.
   * @return Optional containing RateLimitInfo if registered.
   */
  Optional<RateLimitInfo> getRateLimit(String tierKey);

  /**
   * Resolves the tier key flagged as the system default in the database. The default tier selection
   * is data, not configuration — it lives in the {@code rate_limits} table, not in yaml.
   *
   * @return the default tier key, or empty if none is flagged.
   */
  Optional<String> getDefaultTierKey();
}
