package com.krizaka.test.architecture.fixture.security.good;

import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/** A security chain that reads the bearer token from the Authorization header only. */
public class HeaderTokenChain {

  public DefaultBearerTokenResolver resolver() {
    return new DefaultBearerTokenResolver();
  }
}
