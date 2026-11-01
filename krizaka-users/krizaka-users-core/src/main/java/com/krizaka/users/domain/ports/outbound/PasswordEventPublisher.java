package com.krizaka.users.domain.ports.outbound;

import com.krizaka.users.domain.model.PasswordResetRequestedEvent;

/** Outbound port interface for publishing password-related events. */
public interface PasswordEventPublisher {

  /**
   * Publishes the password reset requested event downstream.
   *
   * @param event the PasswordResetRequestedEvent to publish
   */
  void publish(PasswordResetRequestedEvent event);
}
