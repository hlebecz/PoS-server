package kurs.backend.domain.dto.request;

public abstract class BaseRequest {
  public abstract void validate();

  protected void require(Object value, String fieldName) {
    if (value == null) throw new IllegalArgumentException(fieldName + " обязателен");
  }

  protected void requireNotBlank(String value, String fieldName) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException(fieldName + " не может быть пустым");
  }

  protected void requireMaxLength(String value, int max, String fieldName) {
    if (value != null && value.length() > max)
      throw new IllegalArgumentException(fieldName + ": максимум " + max + " символов");
  }
}
