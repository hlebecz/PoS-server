package kurs.backend.service;

import java.util.List;
import java.util.UUID;

import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.UserDao;
import kurs.backend.domain.persistence.entity.User;

/** Управление учётными записями. Полный CRUD доступен только ADMIN. */
public class UserService {

  private final UserDao userDao;

  public UserService(UserDao userDao) {
    this.userDao = userDao;
  }

  public List<User> findAll(AuthenticatedUser caller) {
    requireAdmin(caller);
    return userDao.findAll();
  }

  public User findById(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    return userDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Пользователь не найден", "USER_NOT_FOUND"));
  }

  public User create(AuthenticatedUser caller, User user) {
    requireAdmin(caller);
    userDao
        .findByLogin(user.getLogin())
        .ifPresent(
            u -> {
              throw new ServiceException("Логин уже занят", "USER_LOGIN_TAKEN");
            });
    return userDao.save(user);
  }

  public User createGueust(User user) {
    userDao
        .findByLogin(user.getLogin())
        .ifPresent(
            u -> {
              throw new ServiceException("Логин уже занят", "USER_LOGIN_TAKEN");
            });
    return userDao.save(user);
  }

  public User update(AuthenticatedUser caller, User user) {
    requireAdmin(caller);
    userDao
        .findById(user.getId())
        .orElseThrow(() -> new ServiceException("Пользователь не найден", "USER_NOT_FOUND"));
    return userDao.update(user);
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    User user =
        userDao
            .findById(id)
            .orElseThrow(() -> new ServiceException("Пользователь не найден", "USER_NOT_FOUND"));
    userDao.delete(user);
  }

  public User deactivate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    User user =
        userDao
            .findById(id)
            .orElseThrow(() -> new ServiceException("Пользователь не найден", "USER_NOT_FOUND"));
    user.setIsActive(false);
    return userDao.update(user);
  }

  private void requireAdmin(AuthenticatedUser caller) {
    if (!caller.isAdmin()) {
      throw new AccessDeniedException("Требуется роль ADMIN");
    }
  }
}
