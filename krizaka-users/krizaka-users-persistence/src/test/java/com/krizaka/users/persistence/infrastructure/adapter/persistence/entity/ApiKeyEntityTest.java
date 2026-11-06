package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiKeyEntityTest {

  @Test
  @DisplayName("Getters return the values set on the entity")
  void gettersAndSetters() {
    Instant created = Instant.parse("2026-01-01T00:00:00Z");
    Instant used = Instant.parse("2026-02-01T00:00:00Z");

    ApiKeyEntity entity = new ApiKeyEntity();
    entity.setId("key-1");
    entity.setUserId("user-1");
    entity.setName("CI pipeline");
    entity.setKeyPrefix("oz_live_abcd");
    entity.setKeyHash("hash");
    entity.setCreatedAt(created);
    entity.setLastUsedAt(used);

    assertThat(entity.getId()).isEqualTo("key-1");
    assertThat(entity.getUserId()).isEqualTo("user-1");
    assertThat(entity.getName()).isEqualTo("CI pipeline");
    assertThat(entity.getKeyPrefix()).isEqualTo("oz_live_abcd");
    assertThat(entity.getKeyHash()).isEqualTo("hash");
    assertThat(entity.getCreatedAt()).isEqualTo(created);
    assertThat(entity.getLastUsedAt()).isEqualTo(used);
  }
}
