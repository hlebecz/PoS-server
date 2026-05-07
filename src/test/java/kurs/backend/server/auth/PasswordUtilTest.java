package kurs.backend.server.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordUtilTest {

  @Test
  void hash_shouldReturnNonNullHash() {
    String plaintext = "myPassword123";
    String hash = PasswordUtil.hash(plaintext);

    assertNotNull(hash);
    assertFalse(hash.isEmpty());
  }

  @Test
  void hash_shouldReturnDifferentHashesForSamePassword() {
    String plaintext = "myPassword123";
    String hash1 = PasswordUtil.hash(plaintext);
    String hash2 = PasswordUtil.hash(plaintext);

    assertNotEquals(hash1, hash2, "BCrypt should generate different salts");
  }

  @Test
  void hash_shouldStartWithBCryptPrefix() {
    String plaintext = "myPassword123";
    String hash = PasswordUtil.hash(plaintext);

    assertTrue(hash.startsWith("$2a$"), "BCrypt hash should start with $2a$");
  }

  @Test
  void verify_shouldReturnTrueForCorrectPassword() {
    String plaintext = "myPassword123";
    String hash = PasswordUtil.hash(plaintext);

    assertTrue(PasswordUtil.verify(plaintext, hash));
  }

  @Test
  void verify_shouldReturnFalseForIncorrectPassword() {
    String plaintext = "myPassword123";
    String hash = PasswordUtil.hash(plaintext);

    assertFalse(PasswordUtil.verify("wrongPassword", hash));
  }

  @Test
  void verify_shouldReturnFalseForInvalidHash() {
    String plaintext = "myPassword123";
    String invalidHash = "not-a-valid-bcrypt-hash";

    assertFalse(PasswordUtil.verify(plaintext, invalidHash));
  }

  @Test
  void verify_shouldReturnFalseForNullHash() {
    String plaintext = "myPassword123";

    assertFalse(PasswordUtil.verify(plaintext, null));
  }

  @Test
  void verify_shouldReturnFalseForEmptyHash() {
    String plaintext = "myPassword123";

    assertFalse(PasswordUtil.verify(plaintext, ""));
  }

  @Test
  void verify_shouldHandleEmptyPassword() {
    String plaintext = "";
    String hash = PasswordUtil.hash(plaintext);

    assertTrue(PasswordUtil.verify(plaintext, hash));
    assertFalse(PasswordUtil.verify("notEmpty", hash));
  }

  @Test
  void verify_shouldHandleSpecialCharacters() {
    String plaintext = "p@ssw0rd!#$%^&*()";
    String hash = PasswordUtil.hash(plaintext);

    assertTrue(PasswordUtil.verify(plaintext, hash));
  }

  @Test
  void verify_shouldHandleUnicodeCharacters() {
    String plaintext = "пароль密码🔒";
    String hash = PasswordUtil.hash(plaintext);

    assertTrue(PasswordUtil.verify(plaintext, hash));
  }

  @Test
  void verify_shouldBeCaseSensitive() {
    String plaintext = "MyPassword123";
    String hash = PasswordUtil.hash(plaintext);

    assertTrue(PasswordUtil.verify("MyPassword123", hash));
    assertFalse(PasswordUtil.verify("mypassword123", hash));
    assertFalse(PasswordUtil.verify("MYPASSWORD123", hash));
  }
}
