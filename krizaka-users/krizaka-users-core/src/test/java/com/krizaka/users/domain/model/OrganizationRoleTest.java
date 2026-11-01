package com.krizaka.users.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class OrganizationRoleTest {

  @Test
  void onlyOwnersAndAdministratorsManage() {
    assertThat(OrganizationRole.OWNER.canManage()).isTrue();
    assertThat(OrganizationRole.ADMIN.canManage()).isTrue();
    assertThat(OrganizationRole.MEMBER.canManage()).isFalse();
    assertThat(OrganizationRole.SERVICE.canManage()).isFalse();
  }

  @Test
  void aServiceAccountIsNotAPerson() {
    assertThat(OrganizationRole.SERVICE.isPerson()).isFalse();
    assertThat(OrganizationRole.MEMBER.isPerson()).isTrue();
  }

  @Test
  void recordsRejectMissingFields() {
    assertThatThrownBy(() -> new Organization(null, "s", "n", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new OrganizationMember("o", "u", null, null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new OrganizationActor(null, false))
        .isInstanceOf(NullPointerException.class);
  }
}
