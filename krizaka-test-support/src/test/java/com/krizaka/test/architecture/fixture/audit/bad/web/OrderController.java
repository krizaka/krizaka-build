package com.krizaka.test.architecture.fixture.audit.bad.web;

import com.krizaka.test.architecture.fixture.audit.bad.persistence.OrderStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Reaches the store, keeps a mutable field and does not validate its body. */
@RestController
public class OrderController {

  private OrderStore store = new OrderStore();

  @PostMapping("/orders")
  public String create(@RequestBody OrderDto body) {
    return store.find(body.sku());
  }
}
