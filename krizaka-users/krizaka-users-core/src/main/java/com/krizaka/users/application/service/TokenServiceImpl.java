package com.krizaka.users.application.service;

import com.krizaka.users.domain.model.User;
import com.krizaka.users.domain.ports.inbound.TokenService;
import com.krizaka.users.infrastructure.config.JwtProperties;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Package-private implementation of the session-token issuer (HS256, claims-enriched). */
@Service
class TokenServiceImpl implements TokenService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;

  TokenServiceImpl(JwtEncoder identityJwtEncoder, JwtProperties properties) {
    this.jwtEncoder = Objects.requireNonNull(identityJwtEncoder, "JwtEncoder cannot be null");
    this.properties = Objects.requireNonNull(properties, "JwtProperties cannot be null");
  }

  @Override
  public String issue(User user) {
    Objects.requireNonNull(user, "User cannot be null");
    Instant now = Instant.now();
    JwtClaimsSet.Builder claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .subject(user.id().toString())
            .issuedAt(now)
            .expiresAt(now.plus(properties.ttl()))
            .claim("username", user.username())
            .claim("email", user.email())
            .claim("roles", List.copyOf(user.authorities()));
    if (user.rateLimitTier() != null && !user.rateLimitTier().isBlank()) {
      claims.claim("tier", user.rateLimitTier());
    }
    return jwtEncoder.encode(JwtEncoderParameters.from(header(), claims.build())).getTokenValue();
  }

  /** ES256 naming the kid when the issuer key is configured (ADR-001), legacy HS256 otherwise. */
  private JwsHeader header() {
    if (properties.signing() == null || !properties.signing().configured()) {
      return JwsHeader.with(MacAlgorithm.HS256).build();
    }
    return JwsHeader.with(SignatureAlgorithm.ES256).keyId(properties.signing().keyId()).build();
  }
}
