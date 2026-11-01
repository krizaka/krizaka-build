package com.krizaka.users.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RoleTest {

  @Test
  void admin_returnsAdminName() {
    Role admin = new Role.Admin();
    assertEquals("ADMIN", admin.name());
  }

  @Test
  void user_returnsUserName() {
    Role user = new Role.User();
    assertEquals("USER", user.name());
  }

  @Test
  void guest_returnsGuestName() {
    Role guest = new Role.Guest();
    assertEquals("GUEST", guest.name());
  }

  @Test
  void sealedInterface_permitsOnlyKnownSubtypes() {
    assertInstanceOf(Role.class, new Role.Admin());
    assertInstanceOf(Role.class, new Role.User());
    assertInstanceOf(Role.class, new Role.Guest());
  }

  @Test
  void equalsAndHashCode_forRecordRoles() {
    assertEquals(new Role.Admin(), new Role.Admin());
    assertEquals(new Role.User(), new Role.User());
    assertEquals(new Role.Guest(), new Role.Guest());
  }
}
