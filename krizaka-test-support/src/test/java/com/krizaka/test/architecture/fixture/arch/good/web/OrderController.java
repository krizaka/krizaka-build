package com.krizaka.test.architecture.fixture.arch.good.web;

import com.krizaka.test.architecture.fixture.arch.good.service.OrderService;
import org.springframework.web.bind.annotation.RestController;

/** Web calls service. */
@RestController
public class OrderController {
  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  public String find() {
    return service.find();
  }
}
