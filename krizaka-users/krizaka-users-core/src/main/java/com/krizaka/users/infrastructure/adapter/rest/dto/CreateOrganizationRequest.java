package com.krizaka.users.infrastructure.adapter.rest.dto;

/**
 * Request to create an organization.
 *
 * @param slug The unique handle.
 * @param name The display name.
 */
public record CreateOrganizationRequest(String slug, String name) {}
