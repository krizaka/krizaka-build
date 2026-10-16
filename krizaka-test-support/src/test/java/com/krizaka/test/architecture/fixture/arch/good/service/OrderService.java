package com.krizaka.test.architecture.fixture.arch.good.service;

import com.krizaka.test.architecture.fixture.arch.good.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service calls repository; the transaction starts here. */
@Service
public class OrderService {
  private final OrderRepository repository;

  public OrderService(OrderRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public String find() {
    return repository.load();
  }
}
