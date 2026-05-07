package kurs.backend;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that sets up test environment variables before tests run. This ensures
 * ServerConfig can initialize properly with required JWT_SECRET.
 */
public class TestConfigExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    // Set required environment variables for tests
    setEnvIfNotSet("JWT_SECRET", "test-secret-key-for-unit-tests-only");
    setEnvIfNotSet("POSTGRES_PASSWORD", "test-password");
    setEnvIfNotSet("DB_HOST", "localhost");
    setEnvIfNotSet("POSTGRES_USER", "test-user");
    setEnvIfNotSet("POSTGRES_DB", "test-db");
  }

  private void setEnvIfNotSet(String key, String value) {
    if (System.getenv(key) == null) {
      try {
        // Use reflection to set environment variable (Java doesn't provide direct API)
        java.util.Map<String, String> env = System.getenv();
        java.lang.reflect.Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) field.get(env);
        map.put(key, value);
      } catch (Exception e) {
        // If reflection fails, just log and continue
        System.err.println("Warning: Could not set environment variable " + key);
      }
    }
  }
}
