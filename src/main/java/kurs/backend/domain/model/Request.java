package kurs.backend.domain.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request {

  private String requestId;
  private RequestType type;
  private String token;
  private String payload;
}
