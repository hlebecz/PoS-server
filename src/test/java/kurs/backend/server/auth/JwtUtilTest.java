package kurs.backend.server.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.exceptions.JWTVerificationException;

import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;

class JwtUtilTest {

  @Test
  void generateToken_shouldReturnNonNullToken() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .login("testuser")
            .role(UserRole.GUEST)
            .passwordHash("hash")
            .isActive(true)
            .build();

    String token = JwtUtil.generateToken(user);

    assertNotNull(token);
    assertFalse(token.isEmpty());
  }

  @Test
  void generateToken_shouldContainThreeParts() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .login("testuser")
            .role(UserRole.GUEST)
            .passwordHash("hash")
            .isActive(true)
            .build();

    String token = JwtUtil.generateToken(user);
    String[] parts = token.split("\\.");

    assertEquals(3, parts.length, "JWT should have 3 parts: header.payload.signature");
  }

  @Test
  void validateToken_shouldReturnAuthenticatedUserForValidToken() {
    UUID userId = UUID.randomUUID();
    User user =
        User.builder()
            .id(userId)
            .login("testuser")
            .role(UserRole.CASHIER)
            .passwordHash("hash")
            .isActive(true)
            .build();

    String token = JwtUtil.generateToken(user);
    AuthenticatedUser authenticatedUser = JwtUtil.validateToken(token);

    assertNotNull(authenticatedUser);
    assertEquals(userId, authenticatedUser.getUserId());
    assertEquals("testuser", authenticatedUser.getUsername());
    assertEquals(UserRole.CASHIER, authenticatedUser.getRole());
  }

  @Test
  void validateToken_shouldThrowExceptionForInvalidToken() {
    String invalidToken = "invalid.token.here";

    assertThrows(JWTVerificationException.class, () -> JwtUtil.validateToken(invalidToken));
  }

  @Test
  void validateToken_shouldThrowExceptionForMalformedToken() {
    String malformedToken = "notajwt";

    assertThrows(JWTVerificationException.class, () -> JwtUtil.validateToken(malformedToken));
  }

  @Test
  void validateToken_shouldThrowExceptionForEmptyToken() {
    assertThrows(JWTVerificationException.class, () -> JwtUtil.validateToken(""));
  }

  @Test
  void validateToken_shouldThrowExceptionForNullToken() {
    assertThrows(JWTVerificationException.class, () -> JwtUtil.validateToken(null));
  }

  @Test
  void generateToken_shouldWorkForAllUserRoles() {
    for (UserRole role : UserRole.values()) {
      User user =
          User.builder()
              .id(UUID.randomUUID())
              .login("user_" + role.name())
              .role(role)
              .passwordHash("hash")
              .isActive(true)
              .build();

      String token = JwtUtil.generateToken(user);
      AuthenticatedUser authenticatedUser = JwtUtil.validateToken(token);

      assertEquals(role, authenticatedUser.getRole());
    }
  }

  @Test
  void generateToken_shouldHandleSpecialCharactersInUsername() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .login("user@example.com")
            .role(UserRole.GUEST)
            .passwordHash("hash")
            .isActive(true)
            .build();

    String token = JwtUtil.generateToken(user);
    AuthenticatedUser authenticatedUser = JwtUtil.validateToken(token);

    assertEquals("user@example.com", authenticatedUser.getUsername());
  }

  @Test
  void validateToken_shouldRejectTokenWithWrongSignature() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .login("testuser")
            .role(UserRole.GUEST)
            .passwordHash("hash")
            .isActive(true)
            .build();

    String token = JwtUtil.generateToken(user);
    // Tamper with the signature
    String[] parts = token.split("\\.");
    String tamperedToken = parts[0] + "." + parts[1] + ".tampered";

    assertThrows(JWTVerificationException.class, () -> JwtUtil.validateToken(tamperedToken));
  }

  @Test
  void expirationSeconds_shouldReturnPositiveValue() {
    long expiration = JwtUtil.expirationSeconds();

    assertTrue(expiration > 0, "Expiration should be positive");
  }

  @Test
  void generateToken_shouldGenerateDifferentTokensForSameUser() {
    User user =
        User.builder()
            .id(UUID.randomUUID())
            .login("testuser")
            .role(UserRole.GUEST)
            .passwordHash("hash")
            .isActive(true)
            .build();

    String token1 = JwtUtil.generateToken(user);
    // Small delay to ensure different issuedAt timestamp
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    String token2 = JwtUtil.generateToken(user);

    assertNotEquals(token1, token2, "Tokens should differ due to different issuedAt times");
  }
}
