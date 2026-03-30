package kurs.backend.domain.model;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response implements Serializable {

  private static final long serialVersionUID = 1L;

  private String requestId;
  private boolean success;
  private String message;
  private Object data;
  private String errorCode;
  private long timestamp;

  public static Response ok(String requestId, Object data) {
    return Response.builder()
        .requestId(requestId)
        .success(true)
        .data(data)
        .timestamp(now())
        .build();
  }

  public static Response ok(String requestId, String message, Object data) {
    return Response.builder()
        .requestId(requestId)
        .success(true)
        .message(message)
        .data(data)
        .timestamp(now())
        .build();
  }

  public static Response fail(String requestId, String message) {
    return Response.builder()
        .requestId(requestId)
        .success(false)
        .message(message)
        .timestamp(now())
        .build();
  }

  public static Response fail(String requestId, String message, String errorCode) {
    return Response.builder()
        .requestId(requestId)
        .success(false)
        .message(message)
        .errorCode(errorCode)
        .timestamp(now())
        .build();
  }

  private static long now() {
    return System.currentTimeMillis();
  }
}
