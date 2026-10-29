package com.krizaka.test.architecture.fixture.audit.good.service;

import org.springframework.stereotype.Service;

/** A bean with final collaborators. */
@Service
public class OrderService {

  private final String prefix;

  public OrderService() {
    this.prefix = "order-";
  }

  public String find(String id) {
    return prefix + id;
  }
}
