package com.krizaka.test.architecture.fixture.authz.good;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Fixture: class-level @PreAuthorize covers all endpoints — AuthzRules must accept it. */
@RestController
@PreAuthorize("hasRole('BILLING_ADMIN')")
public class ClassLevelProtectedController {

  @GetMapping("/api/invoices/{id}")
  public String get(@PathVariable String id) {
    return id;
  }
}
