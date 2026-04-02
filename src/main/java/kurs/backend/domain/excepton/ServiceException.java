package kurs.backend.domain.excepton;

import lombok.Getter;

public class ServiceException extends RuntimeException {
  @Getter private final String errorCode;

  public ServiceException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
  }
}
