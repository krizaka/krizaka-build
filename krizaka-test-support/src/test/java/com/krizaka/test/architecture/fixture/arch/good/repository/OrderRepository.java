package com.krizaka.test.architecture.fixture.arch.good.repository;

import org.springframework.stereotype.Repository;

/** The one place that reads. */
@Repository
public class OrderRepository {
  public String load() {
    return "order";
  }
}
