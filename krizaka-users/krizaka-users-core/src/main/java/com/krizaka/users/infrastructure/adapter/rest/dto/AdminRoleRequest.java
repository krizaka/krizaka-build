package com.krizaka.users.infrastructure.adapter.rest.dto;

/** Grants ({@code true}) or withdraws ({@code false}) the administrator role. */
public record AdminRoleRequest(boolean admin) {}
