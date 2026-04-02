package kurs.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import lombok.AllArgsConstructor;

import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.TimesheetDao;
import kurs.backend.domain.persistence.dao.UserDao;
import kurs.backend.domain.persistence.entity.Employee;
import kurs.backend.domain.persistence.entity.Timesheet;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;
import kurs.backend.server.auth.JwtUtil;
import kurs.backend.server.auth.PasswordUtil;

/**
 * Аутентификация и управление сессией.
 *
 * <p>При логине создаёт/открывает запись Timesheet для сотрудника (все роли кроме GUEST). При
 * логауте закрывает Timesheet, вычисляя hoursWorked.
 */
@AllArgsConstructor
public class AuthService {

  private final UserDao userDao;
  private final EmployeeDao employeeDao;
  private final TimesheetDao timesheetDao;
  private final UserService userService;

  // -----------------------------------------------------------------------
  // Login
  // -----------------------------------------------------------------------

  /**
   * Проверяет логин/пароль, генерирует JWT. Для не-GUEST сотрудников открывает запись Timesheet на
   * сегодня.
   *
   * @return JWT-токен
   */
  public String login(String login, String rawPassword) {
    User user =
        userDao
            .findByLogin(login)
            .orElseThrow(() -> new ServiceException("Неверный логин или пароль", "AUTH_INVALID"));

    if (!user.getIsActive()) {
      throw new ServiceException("Учётная запись заблокирована", "AUTH_INACTIVE");
    }

    if (!verifyPassword(rawPassword, user.getPasswordHash())) {
      throw new ServiceException("Неверный логин или пароль", "AUTH_INVALID");
    }

    if (user.getRole() != UserRole.GUEST) {
      openTimesheetIfAbsent(user);
    }

    return JwtUtil.generateToken(user);
  }

  // -----------------------------------------------------------------------
  // Logout
  // -----------------------------------------------------------------------

  /**
   * Закрывает Timesheet (проставляет check_out и hours_worked). Токен после этого должен быть
   * инвалидирован на уровне сервера/клиента.
   */
  public void logout(AuthenticatedUser caller) {
    if (caller.getRole() == UserRole.GUEST) return;

    Employee employee =
        employeeDao
            .findByUserId(caller.getUserId())
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Сотрудник для пользователя не найден", "EMPLOYEE_NOT_FOUND"));

    LocalDate today = LocalDate.now();
    timesheetDao.findByEmployeeIdAndPeriod(employee.getId(), today, today).stream()
        .filter(t -> t.getCheckOut() == null)
        .findFirst()
        .ifPresent(
            t -> {
              LocalTime now = LocalTime.now();
              t.setCheckOut(now);
              t.setHoursWorked(computeHours(t.getCheckIn(), now));
              timesheetDao.update(t);
            });
  }

  public void register(String login, String rawPassword) {
    User user =
        User.builder()
            .login(login)
            .passwordHash(PasswordUtil.hash(rawPassword))
            .isActive(true)
            .build();
    try {
      userService.createGueust(user);
    } catch (ServiceException e) {
      throw e;
    }
  }

  // -----------------------------------------------------------------------
  // Internal helpers
  // -----------------------------------------------------------------------

  private void openTimesheetIfAbsent(User user) {
    Optional<Employee> employeeOpt = employeeDao.findByUserId(user.getId());
    if (employeeOpt.isEmpty()) return; // пользователь есть, но ещё не привязан к сотруднику

    Employee employee = employeeOpt.get();
    LocalDate today = LocalDate.now();

    boolean alreadyOpen =
        timesheetDao.findByEmployeeIdAndPeriod(employee.getId(), today, today).stream()
            .anyMatch(t -> t.getCheckOut() == null);

    if (!alreadyOpen) {
      Timesheet ts =
          Timesheet.builder().employee(employee).workDate(today).checkIn(LocalTime.now()).build();
      timesheetDao.save(ts);
    }
  }

  private java.math.BigDecimal computeHours(LocalTime checkIn, LocalTime checkOut) {
    if (checkIn == null) return java.math.BigDecimal.ZERO;
    long minutes = java.time.Duration.between(checkIn, checkOut).toMinutes();
    return java.math.BigDecimal.valueOf(minutes)
        .divide(java.math.BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
  }

  /** Заглушка — замени на BCrypt или другой алгоритм хэширования. */
  private boolean verifyPassword(String raw, String hash) {
    return PasswordUtil.verify(raw, hash);
  }
}
