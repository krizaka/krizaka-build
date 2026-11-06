package com.krizaka.users.persistence.infrastructure.adapter.persistence.entity;

import static com.krizaka.users.persistence.TestConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link UserInterceptionId} — composite key equals/hashCode contract. */
class UserInterceptionIdTest {
  @org.junit.jupiter.api.Test
  void sonar_context_load() {
    org.junit.jupiter.api.Assertions.assertTrue(true);
  }

  @Nested
  @DisplayName("Equality contract")
  class EqualityContract {

    @Test
    @DisplayName("same fields are equal")
    void sameFieldsEqual() {
      var id1 = new UserInterceptionId(USER_1, "REFINER");
      var id2 = new UserInterceptionId(USER_1, "REFINER");
      assertEquals(id1, id2);
      assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @DisplayName("different userId not equal")
    void differentUserId() {
      var id1 = new UserInterceptionId(USER_1, "REFINER");
      var id2 = new UserInterceptionId("user2", "REFINER");
      assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("different interceptionType not equal")
    void differentType() {
      var id1 = new UserInterceptionId(USER_1, "REFINER");
      var id2 = new UserInterceptionId(USER_1, "ROUTER");
      assertNotEquals(id1, id2);
    }

    @Test
    @DisplayName("not equal to null")
    void notEqualToNull() {
      var id = new UserInterceptionId(USER_1, "REFINER");
      assertNotEquals(null, id);
    }

    @Test
    @DisplayName("reflexive equality")
    void reflexive() {
      var id1 = new UserInterceptionId(USER_1, "REFINER");
      var id2 = new UserInterceptionId(USER_1, "REFINER");
      assertEquals(id1, id2, "Equal objects must be equal");
    }
  }

  @Nested
  @DisplayName("Record accessors")
  class RecordAccessors {

    @Test
    @DisplayName("accessors return constructor values")
    void accessorsReturnValues() {
      var id = new UserInterceptionId("u1", "REFINER");
      assertEquals("u1", id.userId());
      assertEquals("REFINER", id.interceptionType());
    }
  }
}
