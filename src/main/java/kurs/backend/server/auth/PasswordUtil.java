package kurs.backend.server.auth;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

  private PasswordUtil() {}

  private static final int ROUNDS = 12;

  public static String hash(String plaintext) {
    return BCrypt.hashpw(plaintext, BCrypt.gensalt(ROUNDS));
  }

  public static boolean verify(String plaintext, String hashed) {
    try {
      return BCrypt.checkpw(plaintext, hashed);
    } catch (Exception e) {
      return false;
    }
  }
}
