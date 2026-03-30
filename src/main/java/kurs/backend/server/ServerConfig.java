package kurs.backend.server;

import java.util.ResourceBundle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerConfig {

  private static final Logger log = LogManager.getLogger(ServerConfig.class);
  private static final ResourceBundle bundle = ResourceBundle.getBundle("server");
  private static final ServerConfig INSTANCE = new ServerConfig();

  private ServerConfig() {}

  public static ServerConfig getInstance() {
    return INSTANCE;
  }

  public String getDbHost() {
    return env("DB_HOST", "db");
  }

  public String getDbUser() {
    return env("POSTGRES_USER", "app");
  }

  public String getDbPassword() {
    return env("POSTGRES_PASSWORD");
  }

  public String getDbName() {
    return env("POSTGRES_DB", "default");
  }

  public String getPgAdminPassword() {
    return env("PGADMIN_DEFAULT_PASSWORD");
  }

  public String getPgAdminEmail() {
    return env("PGADMIN_DEFAULT_EMAIL", "gleb.sidorenko007@gmail.com");
  }

  public String getJWTSecret() {
    return env("JWT_SECRET");
  }

  public Integer getJWTExpiration() {
    return Integer.parseInt(bundle.getString("jwt_expiration_hours"), 24);
  }

  public Integer getPort() {
    return Integer.parseInt(bundle.getString("port"), 8080);
  }

  private String env(String key) {
    String v = System.getenv(key);
    return (v != null && !v.isBlank()) ? v : null;
  }

  private String env(String key, String defaultVal) {
    String v = System.getenv(key);
    return (v != null && !v.isBlank()) ? v : defaultVal;
  }

  private int parseInt(String key, String value, int defaultValue) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      log.error("Invalid number for {}: '{}', using default {}", key, value, defaultValue);
      return defaultValue;
    }
  }

  private long parseLong(String key, String value, long defaultValue) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      log.error("Invalid number for {}: '{}', using default {}", key, value, defaultValue);
      return defaultValue;
    }
  }
}
