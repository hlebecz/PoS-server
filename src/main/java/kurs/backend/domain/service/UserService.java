package kurs.backend.domain.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final Logger log = LogManager.getLogger(UserService.class);
  private static final Logger auditLog = LogManager.getLogger("kurs.backend.audit");

  @Getter private final UserDao userDao;
  private final EmployeeDao employeeDao;

  public List<UserResponse> findAll(AuthenticatedUser caller) {
    requireAdmin(caller);
    log.debug("Finding all users: requestedBy={}", caller.getUserId());
    List<UserResponse> users =
        userDao.findAll().stream().map(this::toResponseWithEmployee).toList();
    log.info("Found {} users", users.size());
    return users;
  }

  public UserResponse findById(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.debug("Finding user by id: userId={}, requestedBy={}", id, caller.getUserId());
    return toResponseWithEmployee(getOrThrow(id));
  }

  public UserResponse create(AuthenticatedUser caller, CreateUserRequest req) {
    requireAdmin(caller);
    req.validate();

    log.info(
        "Creating user: login={}, role={}, requestedBy={}",
        req.getLogin(),
        req.getRole(),
        caller.getUserId());

    userDao
        .findByLogin(req.getLogin())
        .ifPresent(
            u -> {
              log.warn("User creation failed: login already taken - {}", req.getLogin());
              throw new ServiceException("Логин уже занят", "USER_LOGIN_TAKEN");
            });

    User user =
        User.builder()
            .login(req.getLogin())
            .passwordHash(PasswordUtil.hash(req.getPassword()))
            .role(req.getRole())
            .isActive(true)
            .build();

    User savedUser = userDao.save(user);
    log.info(
        "User created successfully: userId={}, login={}, role={}",
        savedUser.getId(),
        savedUser.getLogin(),
        savedUser.getRole());
    auditLog.info(
        "User created: userId={}, login={}, role={}, createdBy={}",
        savedUser.getId(),
        savedUser.getLogin(),
        savedUser.getRole(),
        caller.getUserId());

    return toResponseWithEmployee(savedUser);
  }

  public void createGuest(CreateUserRequest req) {
    req.validate();

    log.info("Creating guest user: login={}", req.getLogin());

    userDao
        .findByLogin(req.getLogin())
        .ifPresent(
            u -> {
              log.warn("Guest creation failed: login already taken - {}", req.getLogin());
              throw new ServiceException("Логин уже занят", "USER_LOGIN_TAKEN");
            });

    User user =
        User.builder()
            .login(req.getLogin())
            .passwordHash(PasswordUtil.hash(req.getPassword()))
            .role(UserRole.GUEST)
            .isActive(true)
            .build();
    User savedUser = userDao.save(user);
    log.info("Guest user created: userId={}, login={}", savedUser.getId(), savedUser.getLogin());
    auditLog.info(
        "Guest user created: userId={}, login={}", savedUser.getId(), savedUser.getLogin());
  }

  public UserResponse update(AuthenticatedUser caller, UpdateUserRequest req) {
    requireAdmin(caller);
    req.validate();

    User user = getOrThrow(req.getId());
    log.info("Updating user: userId={}, requestedBy={}", req.getId(), caller.getUserId());

    if (req.getLogin() != null && !req.getLogin().isBlank()) {
      userDao
          .findByLogin(req.getLogin())
          .filter(u -> !u.getId().equals(req.getId()))
          .ifPresent(
              u -> {
                log.warn("User update failed: login already taken - {}", req.getLogin());
                throw new ServiceException("Логин уже занят", "USER_LOGIN_TAKEN");
              });
      user.setLogin(req.getLogin());
    }
    if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
      user.setPasswordHash(PasswordUtil.hash(req.getNewPassword()));
      log.debug("Password updated for userId={}", req.getId());
    }
    if (req.getRole() != null) {
      log.debug("Role changed for userId={}: {} -> {}", req.getId(), user.getRole(), req.getRole());
      user.setRole(req.getRole());
    }
    if (req.getIsActive() != null) {
      log.debug(
          "Active status changed for userId={}: {} -> {}",
          req.getId(),
          user.getIsActive(),
          req.getIsActive());
      user.setIsActive(req.getIsActive());
    }

    User updatedUser = userDao.update(user);
    log.info("User updated successfully: userId={}", updatedUser.getId());
    auditLog.info(
        "User updated: userId={}, login={}, role={}, isActive={}, updatedBy={}",
        updatedUser.getId(),
        updatedUser.getLogin(),
        updatedUser.getRole(),
        updatedUser.getIsActive(),
        caller.getUserId());

    return toResponseWithEmployee(updatedUser);
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Deleting user: userId={}, requestedBy={}", id, caller.getUserId());
    User user = getOrThrow(id);
    userDao.delete(user);
    log.info("User deleted successfully: userId={}", id);
    auditLog.info(
        "User deleted: userId={}, login={}, deletedBy={}", id, user.getLogin(), caller.getUserId());
  }

  public UserResponse deactivate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Deactivating user: userId={}, requestedBy={}", id, caller.getUserId());
    User user = getOrThrow(id);
    user.setIsActive(false);
    User updatedUser = userDao.update(user);
    log.info("User deactivated successfully: userId={}", id);
    auditLog.info(
        "User deactivated: userId={}, login={}, deactivatedBy={}",
        id,
        user.getLogin(),
        caller.getUserId());
    return toResponseWithEmployee(updatedUser);
  }

  public UserResponse activate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Activating user: userId={}, requestedBy={}", id, caller.getUserId());
    User user = getOrThrow(id);
    user.setIsActive(true);
    User updatedUser = userDao.update(user);
    log.info("User activated successfully: userId={}", id);
    auditLog.info(
        "User activated: userId={}, login={}, activatedBy={}",
        id,
        user.getLogin(),
        caller.getUserId());
    return toResponseWithEmployee(updatedUser);
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
