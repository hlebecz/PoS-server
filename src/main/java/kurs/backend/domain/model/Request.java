package kurs.backend.domain.model;

import java.io.Serializable;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request implements Serializable {

  private static final long serialVersionUID = 1L;

  private String requestId;
  private RequestType type;
  private String token;
  private Object payload;

  public enum RequestType {}
}
