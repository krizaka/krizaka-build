package com.krizaka.test.architecture.fixture.security.bad;

import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/** A security chain that also reads the bearer token from the query string. */
public class QueryTokenChain {

  public DefaultBearerTokenResolver resolver() {
    var resolver = new DefaultBearerTokenResolver();
    resolver.setAllowUriQueryParameter(true);
    return resolver;
  }
}
