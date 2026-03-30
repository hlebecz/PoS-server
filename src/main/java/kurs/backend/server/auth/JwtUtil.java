package kurs.backend.server.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;
import kurs.backend.server.ServerConfig;

public final class JwtUtil {

  private JwtUtil() {}

  private static final ServerConfig CFG = ServerConfig.getInstance();
  private static final Algorithm ALGORITHM = Algorithm.HMAC256(System.getenv("JWT_SECRET"));
  private static final String ISSUER = "point-of-sale-service";

  private static final JWTVerifier VERIFIER = JWT.require(ALGORITHM).withIssuer(ISSUER).build();

  public static String generateToken(User user) {
    Instant now = Instant.now();
    Instant expiry = now.plus(CFG.getJWTExpiration(), ChronoUnit.HOURS);
    return JWT.create()
        .withIssuer(ISSUER)
        .withSubject(user.getId().toString())
        .withClaim("username", user.getLogin())
        .withClaim("role", user.getRole().name())
        .withIssuedAt(Date.from(now))
        .withExpiresAt(Date.from(expiry))
        .sign(ALGORITHM);
  }

  public static AuthenticatedUser validateToken(String token) throws JWTVerificationException {
    DecodedJWT jwt = VERIFIER.verify(token);
    return AuthenticatedUser.builder()
        .userId(UUID.fromString(jwt.getSubject()))
        .username(jwt.getClaim("username").asString())
        .role(UserRole.valueOf(jwt.getClaim("role").asString()))
        .build();
  }

  public static long expirationSeconds() {
    return CFG.getJWTExpiration() * 3_600L;
  }
}
