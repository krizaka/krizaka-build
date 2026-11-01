package com.krizaka.users.domain.model;

/**
 * Event published when a new user registers in the Orazaka identity module.
 *
 * @param user The registered user domain object.
 * @param plaintextToken The raw plaintext verification token, if verification is enabled; {@code
 *     null} otherwise.
 */
public record UserRegisteredEvent(User user, String plaintextToken) {}
