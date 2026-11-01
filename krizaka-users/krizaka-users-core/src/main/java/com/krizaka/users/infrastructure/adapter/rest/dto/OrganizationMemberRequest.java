package com.krizaka.users.infrastructure.adapter.rest.dto;

/**
 * Request to add a person to an organization, or to change their role.
 *
 * @param userId The person's user id.
 * @param role {@code OWNER}, {@code ADMIN} or {@code MEMBER}.
 */
public record OrganizationMemberRequest(String userId, String role) {}
