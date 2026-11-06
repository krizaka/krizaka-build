package com.krizaka.users.persistence.domain.model;

import java.io.Serializable;
import java.util.Objects;

/** Clean domain DTO representing User Authority, satisfying ERR-106. */
public record AuthorityDto(Long id, String userId, String authorityName) implements Serializable {

  public AuthorityDto {
    Objects.requireNonNull(userId, "userId cannot be null");
    Objects.requireNonNull(authorityName, "authorityName cannot be null");
  }
}
