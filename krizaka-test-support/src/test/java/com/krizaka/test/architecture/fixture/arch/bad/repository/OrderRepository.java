package com.krizaka.test.architecture.fixture.arch.bad.repository;

import com.krizaka.test.architecture.fixture.arch.bad.service.OrderManager;
import org.springframework.stereotype.Repository;

/** A repository that calls up into a service. */
@Repository
public class OrderRepository {
  public String load() {
    return new OrderManager().home();
  }
}
