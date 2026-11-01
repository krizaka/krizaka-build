package com.krizaka.users.infrastructure.adapter.rest.dto;

import java.util.Objects;

/**
 * Generic message response for endpoints that return informational text.
 *
 * @param message The response message.
 */
public record GenericMessageResponse(String message) {
  /** Compact constructor enforcing non-null message. */
  public GenericMessageResponse {
    Objects.requireNonNull(message, "Message is required");
  }
}
