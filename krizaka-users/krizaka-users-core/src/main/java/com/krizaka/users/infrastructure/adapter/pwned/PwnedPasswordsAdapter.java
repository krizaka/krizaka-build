package com.krizaka.users.infrastructure.adapter.pwned;

import com.krizaka.users.domain.ports.outbound.BreachedPasswordPort;
import com.krizaka.users.infrastructure.config.PasswordProperties;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Pwned Passwords range API with padding: only the first five hex characters of the SHA-1 leave the
 * service, the padded answer hides how many suffixes match. Fail-open: an unreachable service
 * answers « not breached » and the line says so (never the password, never its hash).
 */
@Component
class PwnedPasswordsAdapter implements BreachedPasswordPort {

  private static final Logger LOG = LoggerFactory.getLogger(PwnedPasswordsAdapter.class);
  private static final int PREFIX = 5;

  private final RestClient client;
  private final boolean enabled;
  private final String rangeUrl;

  @org.springframework.beans.factory.annotation.Autowired
  PwnedPasswordsAdapter(PasswordProperties properties) {
    this(
        RestClient.builder()
            .requestFactory(timeouts())
            .defaultHeader("Add-Padding", "true")
            .build(),
        properties);
  }

  PwnedPasswordsAdapter(RestClient client, PasswordProperties properties) {
    this.client = client;
    this.enabled = properties.breachCheck();
    this.rangeUrl = properties.breachRangeUrl();
  }

  private static JdkClientHttpRequestFactory timeouts() {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    factory.setReadTimeout(Duration.ofSeconds(3));
    return factory;
  }

  @Override
  public boolean isBreached(String password) {
    if (!enabled) {
      return false;
    }
    String sha1 = sha1(password);
    String prefix = sha1.substring(0, PREFIX);
    String suffix = sha1.substring(PREFIX);
    try {
      String body = client.get().uri(rangeUrl + prefix).retrieve().body(String.class);
      return body != null && body.lines().anyMatch(line -> matches(line, suffix));
    } catch (RestClientException e) {
      LOG.warn(
          "Breached-password check unavailable ({}): accepted, checked again at next sign-in",
          e.getClass().getSimpleName());
      return false;
    }
  }

  /** {@code SUFFIX:COUNT}; a padded line has a count of 0 and is not a match. */
  private static boolean matches(String line, String suffix) {
    int colon = line.indexOf(':');
    return colon > 0
        && line.substring(0, colon).trim().equalsIgnoreCase(suffix)
        && !"0".equals(line.substring(colon + 1).trim());
  }

  /**
   * SHA-1 is the identifier the Pwned Passwords range API defines (k-anonymity): it names a
   * password in a public breach corpus and protects nothing; only its first five hex characters
   * leave the process. It is not used to store or verify a credential (that is Argon2id).
   */
  private static String sha1(String password) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-1") // NOSONAR java:S4790 protocol-mandated, see above
              .digest(password.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().withUpperCase().formatHex(digest).toUpperCase(Locale.ROOT);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-1 unavailable", e);
    }
  }
}
