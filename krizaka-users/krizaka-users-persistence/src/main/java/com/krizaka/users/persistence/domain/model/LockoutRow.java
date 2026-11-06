package com.krizaka.users.persistence.domain.model;

import java.time.Instant;

/** One lockout counter row; {@code lockedUntil} is {@code null} while not locked. */
public record LockoutRow(int failures, Instant lastFailureAt, Instant lockedUntil) {}
