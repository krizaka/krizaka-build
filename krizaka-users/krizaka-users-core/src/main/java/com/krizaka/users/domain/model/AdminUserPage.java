package com.krizaka.users.domain.model;

import java.util.List;

/** One page of accounts and how many match in all. */
public record AdminUserPage(List<AdminUserView> content, long total, int page, int size) {

  public AdminUserPage {
    content = content == null ? List.of() : List.copyOf(content);
  }
}
