package kurs.backend.domain.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
  private UUID id;
  private String login;
  private UserRole role;
  private boolean isActive;
  private LocalDateTime createdAt;

  public static UserResponse from(User u) {
    return UserResponse.builder()
        .id(u.getId())
        .login(u.getLogin())
        .role(u.getRole())
        .isActive(u.getIsActive())
        .createdAt(u.getCreatedAt())
        .build();
  }
}
