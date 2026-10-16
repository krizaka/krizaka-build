package com.krizaka.test.architecture.fixture.arch.bad.web;

import com.krizaka.test.architecture.fixture.arch.bad.repository.OrderRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/** Skips the service layer and opens a transaction. */
@RestController
public class OrderController {
  private final OrderRepository repository = new OrderRepository();

  @Transactional
  public String find() {
    return repository.load();
  }
}
