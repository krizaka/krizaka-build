package com.krizaka.test.architecture.fixture.audit.good.web;

import jakarta.validation.constraints.NotBlank;

/** A request record with its constraint. */
public record OrderDto(@NotBlank String sku) {}
