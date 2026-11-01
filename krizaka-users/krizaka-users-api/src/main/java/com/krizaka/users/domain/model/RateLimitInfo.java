package com.krizaka.users.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable DTO representing a rate limit tier configuration. Enforces compact constructor
 * validation per ERR-106.
 */
public record RateLimitInfo(String tierKey, Integer requestsPerMinute, Integer concurrentJobs)
    implements Serializable {

  public RateLimitInfo {
    Objects.requireNonNull(tierKey, "tierKey cannot be null");
    if (tierKey.isBlank()) {
      throw new IllegalArgumentException("tierKey cannot be blank");
    }
    Objects.requireNonNull(requestsPerMinute, "requestsPerMinute cannot be null");
    Objects.requireNonNull(concurrentJobs, "concurrentJobs cannot be null");
  }
}
