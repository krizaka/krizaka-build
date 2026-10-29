package com.krizaka.test.architecture.fixture.audit.bad.persistence;

/** A store: the controller must not reach it. */
public class OrderStore {
  public String find(String id) {
    return id;
  }
}
