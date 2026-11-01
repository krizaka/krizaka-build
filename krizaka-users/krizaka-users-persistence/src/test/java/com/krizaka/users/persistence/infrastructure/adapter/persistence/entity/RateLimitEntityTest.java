package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link RateLimitEntity} and {@link RateLimitTierEntity}. */
class RateLimitEntityTest {

  @Test
  void rateLimitEntity_roundTrip() {
    RateLimitEntity entity = new RateLimitEntity();
    entity.setTierKey("premium");
    entity.setRequestsPerMinute(100);
    entity.setConcurrentJobs(5);

    assertEquals("premium", entity.getTierKey());
    assertEquals(100, entity.getRequestsPerMinute());
    assertEquals(5, entity.getConcurrentJobs());
  }

  @Test
  void rateLimitTierEntity_roundTrip() {
    RateLimitTierEntity entity = new RateLimitTierEntity();
    entity.setId("tier-1");
    entity.setCapacity(200);
    entity.setRefillTokens(10);
    entity.setRefillSeconds(60);

    assertEquals("tier-1", entity.getId());
    assertEquals(200, entity.getCapacity());
    assertEquals(10, entity.getRefillTokens());
    assertEquals(60, entity.getRefillSeconds());
  }
}
