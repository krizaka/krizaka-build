package com.krizaka.test.architecture.fixture.arch.bad.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** A service with a pattern name, a scattered value and an environment read. */
@Service
public class OrderManager {
  @Value("${orders.page}")
  private int page;

  public String home() {
    return System.getenv("HOME") + page;
  }
}
