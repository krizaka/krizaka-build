package com.krizaka.users.domain.model;

import java.util.Objects;

/**
 * Who performs an administrative action: the signed-in administrator's id and label (their e-mail).
 */
public record AdminActor(String id, String label) {

  public AdminActor {
    Objects.requireNonNull(id, "actor id");
    Objects.requireNonNull(label, "actor label");
  }
}
