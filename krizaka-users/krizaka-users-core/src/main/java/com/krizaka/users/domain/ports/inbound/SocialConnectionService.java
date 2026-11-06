package com.krizaka.users.domain.ports.inbound;

import com.krizaka.users.domain.model.AdminActor;
import com.krizaka.users.domain.model.EnvironmentRegistration;
import com.krizaka.users.domain.model.SocialConnectionView;
import java.util.List;
import java.util.Optional;

/** The identity-provider connections and their sealed secrets. */
public interface SocialConnectionService {

  /** The connections, with « secret set » and never the secret. */
  List<SocialConnectionView> list();

  /**
   * Imports once, from the environment registrations, the connections not yet stored: sealed,
   * source {@code env}, audited. Skipped (with a log line) while no vault key is configured.
   *
   * @return how many connections were imported
   */
  int importFromEnvironment(AdminActor actor, List<EnvironmentRegistration> registrations);

  /**
   * Re-wraps every sealed secret under the current vault key (the rotation after a key change).
   *
   * @return how many secrets were re-wrapped
   */
  int rewrapAll(AdminActor actor);

  /** The client secret of {@code provider}, opened here and nowhere else. */
  Optional<String> clientSecret(String provider);
}
