package kurs.backend.domain.service;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

import kurs.backend.domain.dto.request.CreateUserRequest;
import kurs.backend.domain.dto.request.UpdateUserRequest;
import kurs.backend.domain.dto.response.UserResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.UserDao;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;
import kurs.backend.server.auth.PasswordUtil;

/** Управление учётными записями. Полный CRUD только для ADMIN. */
@AllArgsConstructor
public class UserService {

  @Getter private final UserDao userDao;
  private final EmployeeDao employeeDao;

  public List<UserResponse> findAll(AuthenticatedUser caller) {
    requireAdmin(caller);
    return userDao.findAll().stream().map(this::toResponseWithEmployee).toList();
  }

  public UserResponse findById(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    return toResponseWithEmployee(getOrThrow(id));
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
            .passwordHash(PasswordUtil.hash(req.getPassword()))
            .role(req.getRole())
            .isActive(true)
            .build();

    return toResponseWithEmployee(userDao.save(user));
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
            .passwordHash(PasswordUtil.hash(req.getPassword()))
            .role(UserRole.GUEST)
            .isActive(true)
            .build();
    userDao.save(user);
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
      user.setPasswordHash(PasswordUtil.hash(req.getNewPassword()));
    if (req.getRole() != null) user.setRole(req.getRole());
    if (req.getIsActive() != null) user.setIsActive(req.getIsActive());

    return toResponseWithEmployee(userDao.update(user));
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    userDao.delete(getOrThrow(id));
  }

  public UserResponse deactivate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    User user = getOrThrow(id);
    user.setIsActive(false);
    return toResponseWithEmployee(userDao.update(user));
  }

  public UserResponse activate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    User user = getOrThrow(id);
    user.setIsActive(true);
    return toResponseWithEmployee(userDao.update(user));
  }

  private UserResponse toResponseWithEmployee(User user) {
    UserResponse.UserResponseBuilder builder =
        UserResponse.builder()
            .id(user.getId())
            .login(user.getLogin())
            .role(user.getRole())
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt());

    employeeDao
        .findByUserId(user.getId())
        .ifPresent(
            emp -> {
              builder.employeeId(emp.getId());
              builder.employeeName(emp.getFullName());
            });

    return builder.build();
  }

  private User getOrThrow(UUID id) {
    return userDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Пользователь не найден", "USER_NOT_FOUND"));
  }

  private void requireAdmin(AuthenticatedUser caller) {
    if (!caller.isAdmin()) throw new AccessDeniedException("Требуется роль ADMIN");
  }
}
