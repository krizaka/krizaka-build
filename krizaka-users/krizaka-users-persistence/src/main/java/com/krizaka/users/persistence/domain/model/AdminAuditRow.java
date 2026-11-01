package com.krizaka.users.persistence.domain.model;

import java.time.Instant;

/** One line of the administration's append-only audit trail. */
public record AdminAuditRow(
    long id,
    Instant at,
    String actorId,
    String actorLabel,
    String action,
    String targetId,
    String detail,
    String requestId,
    String outcome) {}
