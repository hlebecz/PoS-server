package kurs.backend.domain.model;

import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.UserRole;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {

  private UUID userId;
  private String username;
  private String email;
  private UserRole role;

  public boolean isAdmin() {
    return role == UserRole.ADMIN;
  }

  public boolean isManager() {
    return role == UserRole.MANAGER;
  }

  public boolean isAccountant() {
    return role == UserRole.ACCOUNTANT;
  }

  public boolean isCashier() {
    return role == UserRole.CASHIER;
  }

  public boolean isGuest() {
    return role == UserRole.GUEST;
  }

  public boolean hasRole(UserRole required) {
    return this.role == required;
  }

  public boolean hasAnyRole(UserRole... roles) {
    for (UserRole r : roles) if (this.role == r) return true;
    return false;
  }
}
