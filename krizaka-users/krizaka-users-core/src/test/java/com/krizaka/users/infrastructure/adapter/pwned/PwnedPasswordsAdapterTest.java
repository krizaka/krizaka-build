package com.krizaka.users.infrastructure.adapter.pwned;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.krizaka.users.infrastructure.config.PasswordProperties;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PwnedPasswordsAdapterTest {

  private static final String RANGE = "https://range.test/range/";
  // SHA-1("password") = 5BAA6 1E4C9B93F3F0682250B6CF8331B7EE68FD8
  private static final String SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

  private final RestClient.Builder builder =
      RestClient.builder().defaultHeader("Add-Padding", "true");
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

  private PwnedPasswordsAdapter adapter(boolean enabled) {
    return new PwnedPasswordsAdapter(builder.build(), new PasswordProperties(enabled, RANGE));
  }

  @Test
  void onlyTheFiveCharacterPrefixLeavesAndAMatchingSuffixIsBreached() {
    server
        .expect(requestTo(RANGE + "5BAA6"))
        .andExpect(header("Add-Padding", "true"))
        .andRespond(
            withSuccess(
                "0018A45C4D1DEF81644B54AB7F969B88D65:3\r\n" + SUFFIX + ":3861493\r\n",
                org.springframework.http.MediaType.TEXT_PLAIN));

    assertThat(adapter(true).isBreached("password")).isTrue();
    server.verify();
  }

  @Test
  void aPaddedLineWithACountOfZeroIsNotAMatch() {
    server
        .expect(requestTo(RANGE + "5BAA6"))
        .andRespond(withSuccess(SUFFIX + ":0\r\n", org.springframework.http.MediaType.TEXT_PLAIN));

    assertThat(adapter(true).isBreached("password")).isFalse();
  }

  @Test
  void anUnreachableServiceFailsOpen() {
    server.expect(requestTo(RANGE + "5BAA6")).andRespond(withServerError());

    assertThat(adapter(true).isBreached("password")).isFalse();
  }

  @Test
  void switchedOffItAsksNothing() {
    assertThat(adapter(false).isBreached("password")).isFalse();
    server.verify();
  }
}
