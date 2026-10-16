package com.krizaka.test.architecture.fixture.arch.good.config;

import org.springframework.beans.factory.annotation.Value;

/** Binds a value once, at the edge. */
public class OrderConfig {
  @Value("${orders.page}")
  private int page;

  public int page() {
    return page;
  }
}
