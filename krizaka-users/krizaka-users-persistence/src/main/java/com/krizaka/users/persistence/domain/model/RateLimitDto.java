package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Clean domain DTO representing RateLimit configuration, satisfying ERR-106. */
public record RateLimitDto(String tierKey, Integer requestsPerMinute, Integer concurrentJobs)
    implements Serializable {

  public RateLimitDto {
    Objects.requireNonNull(tierKey, "tierKey cannot be null");
    Objects.requireNonNull(requestsPerMinute, "requestsPerMinute cannot be null");
    Objects.requireNonNull(concurrentJobs, "concurrentJobs cannot be null");
  }
}
