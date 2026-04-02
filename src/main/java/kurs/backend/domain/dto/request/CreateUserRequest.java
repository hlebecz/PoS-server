package kurs.backend.domain.dto.request;

import lombok.*;

import kurs.backend.domain.persistence.entity.UserRole;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest extends BaseRequest {
  private String login;
  private String password;
  private UserRole role;

  @Override
  public void validate() {
    requireNotBlank(login, "login");
    requireMaxLength(login, 100, "login");
    requireNotBlank(password, "password");
    require(role, "role");
  }
}
