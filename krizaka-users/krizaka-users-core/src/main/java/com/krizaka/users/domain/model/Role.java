package com.krizaka.users.domain.model;

/** Domain-driven RBAC hierarchy using Java 21 Sealed Interfaces. */
public sealed interface Role permits Role.Admin, Role.User, Role.Guest {

  /**
   * Resolves the authority or role name identifier.
   *
   * @return The dynamic string representation of the role name.
   */
  String name();

  /**
   * Represents the Admin role in the system. Admins have full access permissions over resources and
   * settings.
   */
  record Admin() implements Role {
    /**
     * Returns the hardcoded role name representation for Admin.
     *
     * @return "ADMIN" string identifier.
     */
    @Override
    public String name() {
      return "ADMIN";
    }
  }

  /**
   * Represents the User role in the system. Users have standard execution and management
   * permissions.
   */
  record User() implements Role {
    /**
     * Returns the hardcoded role name representation for User.
     *
     * @return "USER" string identifier.
     */
    @Override
    public String name() {
      return "USER";
    }
  }

  /**
   * Represents the Guest role in the system. Guests have read-only access with minimal sandbox
   * privileges.
   */
  record Guest() implements Role {
    /**
     * Returns the hardcoded role name representation for Guest.
     *
     * @return "GUEST" string identifier.
     */
    @Override
    public String name() {
      return "GUEST";
    }
  }
}
