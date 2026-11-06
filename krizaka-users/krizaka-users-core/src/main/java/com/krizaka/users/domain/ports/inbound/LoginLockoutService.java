package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.LockoutPolicy;

/**
 * The durable progressive lockout of failed sign-ins (ADR-016 decision 2). A locked account refuses
 * even the right password; the state survives a restart and is shared by every instance.
 */
public interface LoginLockoutService {

  /** Seconds the caller must wait before trying again, or {@code 0} when it may try. */
  long retryAfterSeconds(String account, String ip);

  void recordFailure(String account, String ip);

  /** Clears the account-from-this-address counter; the account-wide one is kept. */
  void recordSuccess(String account, String ip);

  LockoutPolicy policy();

  /** Replaces the policy; audited with who and the old and new figures. */
  LockoutPolicy updatePolicy(AdminActor actor, LockoutPolicy policy);
}
