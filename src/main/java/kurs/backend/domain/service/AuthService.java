package kurs.backend.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final Logger log = LogManager.getLogger(AuthService.class);
  private static final Logger auditLog = LogManager.getLogger("kurs.backend.audit");

  private final UserDao userDao;
  private final EmployeeDao employeeDao;
  private final TimesheetDao timesheetDao;
  private final UserService userService;

  public String login(LoginRequest req) {
    req.validate();
    log.info("Login attempt for user: {}", req.getLogin());

    User user =
        userDao
            .findByLogin(req.getLogin())
            .orElseThrow(
                () -> {
                  log.warn("Login failed: user not found - {}", req.getLogin());
                  auditLog.warn("Failed login attempt for non-existent user: {}", req.getLogin());
                  return new ServiceException("Неверный логин или пароль", "AUTH_INVALID");
                });

    if (!user.getIsActive()) {
      log.warn("Login failed: account inactive - userId={}", user.getId());
      auditLog.warn(
          "Failed login attempt for inactive account: userId={}, login={}",
          user.getId(),
          req.getLogin());
      throw new ServiceException("Учётная запись заблокирована", "AUTH_INACTIVE");
    }

    if (!verifyPassword(req.getPassword(), user.getPasswordHash())) {
      log.warn("Login failed: invalid password - userId={}", user.getId());
      auditLog.warn(
          "Failed login attempt with invalid password: userId={}, login={}",
          user.getId(),
          req.getLogin());
      throw new ServiceException("Неверный логин или пароль", "AUTH_INVALID");
    }

    if (user.getRole() != UserRole.GUEST) openTimesheetIfAbsent(user);

    log.info("Login successful: userId={}, role={}", user.getId(), user.getRole());
    auditLog.info(
        "User logged in: userId={}, login={}, role={}",
        user.getId(),
        req.getLogin(),
        user.getRole());

    return JwtUtil.generateToken(user);
  }

  public void logout(AuthenticatedUser caller) {
    log.info("Logout initiated: userId={}", caller.getUserId());

    if (caller.getRole() == UserRole.GUEST) {
      log.debug("Guest user logout: userId={}", caller.getUserId());
      auditLog.info("User logged out: userId={}, role=GUEST", caller.getUserId());
      return;
    }

    Employee emp =
        employeeDao
            .findByUserId(caller.getUserId())
            .orElseThrow(
                () -> {
                  log.error("Logout failed: employee not found for userId={}", caller.getUserId());
                  return new ServiceException(
                      "Сотрудник для пользователя не найден", "EMPLOYEE_NOT_FOUND");
                });

    LocalDate today = LocalDate.now();
    // Find the most recent open timesheet for today
    timesheetDao.findByEmployeeIdAndPeriod(emp.getId(), today, today).stream()
        .filter(t -> t.getCheckOut() == null)
        .max((t1, t2) -> t1.getCheckIn().compareTo(t2.getCheckIn()))
        .ifPresent(
            t -> {
              LocalTime now = LocalTime.now();
              t.setCheckOut(now);
              t.setHoursWorked(computeHours(t.getCheckIn(), now));
              timesheetDao.update(t);
              log.info(
                  "Timesheet closed: timesheetId={}, employeeId={}, hoursWorked={}",
                  t.getId(),
                  emp.getId(),
                  t.getHoursWorked());
            });

    log.info("Logout successful: userId={}, employeeId={}", caller.getUserId(), emp.getId());
    auditLog.info("User logged out: userId={}, role={}", caller.getUserId(), caller.getRole());
  }

  public void register(LoginRequest req) {
    log.info("Registration attempt for user: {}", req.getLogin());
    CreateUserRequest createUserRequest =
        CreateUserRequest.builder()
            .login(req.getLogin())
            .password(req.getPassword())
            .role(UserRole.GUEST)
            .build();
    try {
      userService.createGuest(createUserRequest);
      log.info("Registration successful: login={}", req.getLogin());
      auditLog.info("New guest user registered: login={}", req.getLogin());
    } catch (ServiceException e) {
      log.warn("Registration failed: login={}, error={}", req.getLogin(), e.getMessage());
      throw e;
    }
  }

  private void openTimesheetIfAbsent(User user) {
    Optional<Employee> empOpt = employeeDao.findByUserId(user.getId());
    if (empOpt.isEmpty()) {
      log.debug("No employee record found for userId={}, skipping timesheet", user.getId());
      return;
    }
    Employee emp = empOpt.get();
    LocalDate today = LocalDate.now();

    // Check if there's already an open timesheet for today
    Optional<Timesheet> openTimesheet =
        timesheetDao.findByEmployeeIdAndPeriod(emp.getId(), today, today).stream()
            .filter(t -> t.getCheckOut() == null)
            .findFirst();

    // Reuse existing open timesheet, or create a new one if none exists
    if (openTimesheet.isEmpty()) {
      timesheetDao.save(
          Timesheet.builder().employee(emp).workDate(today).checkIn(LocalTime.now()).build());
      log.info("Timesheet opened for employeeId={}", emp.getId());
    } else {
      log.debug(
          "Reusing existing open timesheet: timesheetId={}, employeeId={}",
          openTimesheet.get().getId(),
          emp.getId());
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
