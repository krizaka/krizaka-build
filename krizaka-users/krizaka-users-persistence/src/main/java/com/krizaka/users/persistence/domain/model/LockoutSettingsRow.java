package com.krizaka.users.persistence.domain.model;

/** The single row of the lockout thresholds, durations in seconds. */
public record LockoutSettingsRow(
    int threshold,
    int accountThreshold,
    long baseDelaySeconds,
    long maxDelaySeconds,
    long forgetAfterSeconds) {}
