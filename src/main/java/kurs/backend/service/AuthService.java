package kurs.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateUserRequest;
import kurs.backend.domain.dto.request.LoginRequest;
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
 * Аутентификация и управление сессией. login() открывает Timesheet для не-GUEST сотрудников.
 * logout() закрывает Timesheet и вычисляет hoursWorked.
 */
@AllArgsConstructor
public class AuthService {

  private final UserDao userDao;
  private final EmployeeDao employeeDao;
  private final TimesheetDao timesheetDao;
  private final UserService userService;

  public String login(LoginRequest req) {
    req.validate();

    User user =
        userDao
            .findByLogin(req.getLogin())
            .orElseThrow(() -> new ServiceException("Неверный логин или пароль", "AUTH_INVALID"));

    if (!user.getIsActive())
      throw new ServiceException("Учётная запись заблокирована", "AUTH_INACTIVE");

    if (!verifyPassword(req.getPassword(), user.getPasswordHash()))
      throw new ServiceException("Неверный логин или пароль", "AUTH_INVALID");

    if (user.getRole() != UserRole.GUEST) openTimesheetIfAbsent(user);

    return JwtUtil.generateToken(user);
  }

  public void logout(AuthenticatedUser caller) {
    if (caller.getRole() == UserRole.GUEST) return;

    Employee emp =
        employeeDao
            .findByUserId(caller.getUserId())
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Сотрудник для пользователя не найден", "EMPLOYEE_NOT_FOUND"));

    LocalDate today = LocalDate.now();
    timesheetDao.findByEmployeeIdAndPeriod(emp.getId(), today, today).stream()
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

  public void register(LoginRequest req) {
    CreateUserRequest createUserRequest =
        CreateUserRequest.builder().login(req.getLogin()).password(req.getPassword()).build();
    try {
      userService.createGuest(createUserRequest);
    } catch (ServiceException e) {
      throw e;
    }
  }

  private void openTimesheetIfAbsent(User user) {
    Optional<Employee> empOpt = employeeDao.findByUserId(user.getId());
    if (empOpt.isEmpty()) return;
    Employee emp = empOpt.get();
    LocalDate today = LocalDate.now();
    boolean alreadyOpen =
        timesheetDao.findByEmployeeIdAndPeriod(emp.getId(), today, today).stream()
            .anyMatch(t -> t.getCheckOut() == null);
    if (!alreadyOpen) {
      timesheetDao.save(
          Timesheet.builder().employee(emp).workDate(today).checkIn(LocalTime.now()).build());
    }
  }

  private BigDecimal computeHours(LocalTime checkIn, LocalTime checkOut) {
    if (checkIn == null) return BigDecimal.ZERO;
    long minutes = Duration.between(checkIn, checkOut).toMinutes();
    return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
  }

  private boolean verifyPassword(String raw, String hash) {
    return PasswordUtil.verify(raw, hash);
  }
}
