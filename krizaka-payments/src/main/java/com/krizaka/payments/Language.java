package com.krizaka.payments;

/** The language of a hosted page. */
public enum Language {
  EN,
  FR;

  /**
   * The code the processor reads.
   *
   * @return {@code en} or {@code fr}
   */
  public String code() {
    return name().toLowerCase(java.util.Locale.ROOT);
  }
}
