package com.krizaka.users.infrastructure.adapter.rest.dto;

/**
 * Request to attach a product's service account to an organization.
 *
 * @param userId The service account's user id.
 */
public record AttachServiceAccountRequest(String userId) {}
