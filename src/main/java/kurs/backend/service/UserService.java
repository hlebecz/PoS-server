package kurs.backend.service;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateUserRequest;
import kurs.backend.domain.dto.request.UpdateUserRequest;
import kurs.backend.domain.dto.response.UserResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.UserDao;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;

/** Управление учётными записями. Полный CRUD только для ADMIN. */
@AllArgsConstructor
public class UserService {

  private final UserDao userDao;

  public List<UserResponse> findAll(AuthenticatedUser caller) {
    requireAdmin(caller);
    return userDao.findAll().stream().map(UserResponse::from).toList();
  }

  public UserResponse findById(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    return UserResponse.from(getOrThrow(id));
  }

  public UserResponse create(AuthenticatedUser caller, CreateUserRequest req) {
    requireAdmin(caller);
    req.validate();

    userDao
        .findByLogin(req.getLogin())
        .ifPresent(
            u -> {
              throw new ServiceException("Логин уже занят", "USER_LOGIN_TAKEN");
            });

    User user =
        User.builder()
            .login(req.getLogin())
            .passwordHash(hashPassword(req.getPassword()))
            .role(req.getRole())
            .isActive(true)
            .build();

    return UserResponse.from(userDao.save(user));
  }

  public void createGuest(CreateUserRequest req) {
    req.validate();

    userDao
        .findByLogin(req.getLogin())
        .ifPresent(
            u -> {
              throw new ServiceException("Логин уже занят", "USER_LOGIN_TAKEN");
            });

    User user =
        User.builder()
            .login(req.getLogin())
            .passwordHash(hashPassword(req.getPassword()))
            .role(UserRole.GUEST)
            .isActive(true)
            .build();
  }

  public UserResponse update(AuthenticatedUser caller, UpdateUserRequest req) {
    requireAdmin(caller);
    req.validate();

    User user = getOrThrow(req.getId());

    if (req.getLogin() != null && !req.getLogin().isBlank()) {
      userDao
          .findByLogin(req.getLogin())
          .filter(u -> !u.getId().equals(req.getId()))
          .ifPresent(
              u -> {
                throw new ServiceException("Логин уже занят", "USER_LOGIN_TAKEN");
              });
      user.setLogin(req.getLogin());
    }
    if (req.getNewPassword() != null && !req.getNewPassword().isBlank())
      user.setPasswordHash(hashPassword(req.getNewPassword()));
    if (req.getRole() != null) user.setRole(req.getRole());
    if (req.getIsActive() != null) user.setIsActive(req.getIsActive());

    return UserResponse.from(userDao.update(user));
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    userDao.delete(getOrThrow(id));
  }

  public UserResponse deactivate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    User user = getOrThrow(id);
    user.setIsActive(false);
    return UserResponse.from(userDao.update(user));
  }

  private User getOrThrow(UUID id) {
    return userDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Пользователь не найден", "USER_NOT_FOUND"));
  }

  private void requireAdmin(AuthenticatedUser caller) {
    if (!caller.isAdmin()) throw new AccessDeniedException("Требуется роль ADMIN");
  }

  /** Замени на BCrypt.hashpw() в реальном проекте. */
  private String hashPassword(String raw) {
    return raw;
  }
}
