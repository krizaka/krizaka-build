package com.krizaka.test.architecture.fixture.audit.good.web;

import com.krizaka.test.architecture.fixture.audit.good.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Talks to a service, validates its body, keeps nothing that changes. */
@RestController
public class OrderController {

  private final OrderService service;

  public OrderController(OrderService service) {
    this.service = service;
  }

  @PostMapping("/orders")
  public String create(@RequestBody @Valid OrderDto body) {
    return service.find(body.sku());
  }
}
