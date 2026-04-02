package kurs.backend.domain.dto.request;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest extends BaseRequest {
  private String login;
  private String password;

  @Override
  public void validate() {
    requireNotBlank(login, "login");
    requireNotBlank(password, "password");
  }
}
