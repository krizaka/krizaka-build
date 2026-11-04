package com.krizaka.users.infrastructure.adapter.rest.dto;

/**
 * Asks for a grant for one run, on the user's own session.
 *
 * @param runId the run (job, chat turn) the key is released for
 */
public record CredentialGrantRequest(String runId) {}
