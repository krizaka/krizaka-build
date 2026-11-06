package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serial;
import java.io.Serializable;

/** Embeddable composite primary key record for {@link UserInterceptionEntity}. */
@Embeddable
public record UserInterceptionId(
    @Column(name = "user_id", length = 255) String userId,
    @Column(name = "interception_type", length = 100) String interceptionType)
    implements Serializable {

  @Serial private static final long serialVersionUID = 1L;
}
