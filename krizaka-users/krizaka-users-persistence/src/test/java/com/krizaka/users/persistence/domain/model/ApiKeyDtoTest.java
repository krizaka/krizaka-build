package com.krizaka.users.persistence.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiKeyDtoTest {

  @Test
  @DisplayName("Valid DTO retains all fields")
  void validConstruction() {
    Instant now = Instant.now();
    ApiKeyDto dto = new ApiKeyDto("id", "user", "name", "oz_live_abcd", "hash", now, null);

    assertThat(dto.id()).isEqualTo("id");
    assertThat(dto.userId()).isEqualTo("user");
    assertThat(dto.keyPrefix()).isEqualTo("oz_live_abcd");
    assertThat(dto.keyHash()).isEqualTo("hash");
    assertThat(dto.createdAt()).isEqualTo(now);
    assertThat(dto.lastUsedAt()).isNull();
  }

  @Test
  @DisplayName("Null required fields are rejected")
  void nullValidation() {
    assertThatThrownBy(() -> new ApiKeyDto(null, "u", "n", "p", "h", null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("id");
    assertThatThrownBy(() -> new ApiKeyDto("i", null, "n", "p", "h", null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("userId");
    assertThatThrownBy(() -> new ApiKeyDto("i", "u", null, "p", "h", null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("name");
    assertThatThrownBy(() -> new ApiKeyDto("i", "u", "n", null, "h", null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("keyPrefix");
    assertThatThrownBy(() -> new ApiKeyDto("i", "u", "n", "p", null, null, null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("keyHash");
  }
}
