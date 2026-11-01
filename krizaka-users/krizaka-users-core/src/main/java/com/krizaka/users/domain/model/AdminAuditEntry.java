package com.krizaka.users.domain.model;

import java.time.Instant;

/** One line of the administration's audit trail. */
public record AdminAuditEntry(
    long id,
    Instant at,
    String actor,
    String action,
    String targetId,
    String detail,
    String requestId,
    String outcome) {}
