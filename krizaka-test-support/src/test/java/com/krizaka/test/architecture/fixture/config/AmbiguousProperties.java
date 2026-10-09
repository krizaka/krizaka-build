package com.krizaka.test.architecture.fixture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Two constructors and no {@code @ConstructorBinding}: the binder cannot choose. */
@ConfigurationProperties(prefix = "fixture")
public class AmbiguousProperties {

  private final String name;

  public AmbiguousProperties(String name) {
    this.name = name;
  }

  public AmbiguousProperties(String name, int ignored) {
    this(name);
  }

  public String name() {
    return name;
  }
}
