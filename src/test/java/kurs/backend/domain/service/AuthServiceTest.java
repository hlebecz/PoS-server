package kurs.backend.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import kurs.backend.TestDataBuilder;
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
import kurs.backend.server.auth.PasswordUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserDao userDao;
  @Mock private EmployeeDao employeeDao;
  @Mock private TimesheetDao timesheetDao;
  @Mock private UserService userService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService = new AuthService(userDao, employeeDao, timesheetDao, userService);
  }

  @Test
  void login_shouldReturnTokenForValidCredentials() {
    LoginRequest request = LoginRequest.builder().login("testuser").password("password123").build();

    String hashedPassword = PasswordUtil.hash("password123");
    User user = TestDataBuilder.user().login("testuser").passwordHash(hashedPassword).build();

    when(userDao.findByLogin("testuser")).thenReturn(Optional.of(user));

    String token = authService.login(request);

    assertNotNull(token);
    assertFalse(token.isEmpty());
    verify(userDao).findByLogin("testuser");
  }

  @Test
  void login_shouldThrowExceptionForInvalidLogin() {
    LoginRequest request =
        LoginRequest.builder().login("nonexistent").password("password123").build();

    when(userDao.findByLogin("nonexistent")).thenReturn(Optional.empty());

    ServiceException exception =
        assertThrows(ServiceException.class, () -> authService.login(request));

    assertEquals("AUTH_INVALID", exception.getErrorCode());
    assertEquals("Неверный логин или пароль", exception.getMessage());
  }

  @Test
  void login_shouldThrowExceptionForInvalidPassword() {
    LoginRequest request =
        LoginRequest.builder().login("testuser").password("wrongpassword").build();

    String hashedPassword = PasswordUtil.hash("correctpassword");
    User user = TestDataBuilder.user().login("testuser").passwordHash(hashedPassword).build();

    when(userDao.findByLogin("testuser")).thenReturn(Optional.of(user));

    ServiceException exception =
        assertThrows(ServiceException.class, () -> authService.login(request));

    assertEquals("AUTH_INVALID", exception.getErrorCode());
    assertEquals("Неверный логин или пароль", exception.getMessage());
  }

  @Test
  void login_shouldThrowExceptionForInactiveUser() {
    LoginRequest request = LoginRequest.builder().login("testuser").password("password123").build();

    String hashedPassword = PasswordUtil.hash("password123");
    User user =
        TestDataBuilder.user()
            .login("testuser")
            .passwordHash(hashedPassword)
            .isActive(false)
            .build();

    when(userDao.findByLogin("testuser")).thenReturn(Optional.of(user));

    ServiceException exception =
        assertThrows(ServiceException.class, () -> authService.login(request));

    assertEquals("AUTH_INACTIVE", exception.getErrorCode());
    assertEquals("Учётная запись заблокирована", exception.getMessage());
  }

  @Test
  void login_shouldOpenTimesheetForNonGuestEmployee() {
    LoginRequest request = LoginRequest.builder().login("cashier").password("password123").build();

    String hashedPassword = PasswordUtil.hash("password123");
    User user = TestDataBuilder.cashierUser().login("cashier").passwordHash(hashedPassword).build();
    Employee employee = TestDataBuilder.employee().user(user).build();

    when(userDao.findByLogin("cashier")).thenReturn(Optional.of(user));
    when(employeeDao.findByUserId(user.getId())).thenReturn(Optional.of(employee));
    when(timesheetDao.findByEmployeeIdAndPeriod(
            eq(employee.getId()), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of());

    String token = authService.login(request);

    assertNotNull(token);
    verify(timesheetDao).save(any(Timesheet.class));
  }

  @Test
  void login_shouldNotOpenTimesheetIfAlreadyOpen() {
    LoginRequest request = LoginRequest.builder().login("cashier").password("password123").build();

    String hashedPassword = PasswordUtil.hash("password123");
    User user = TestDataBuilder.cashierUser().login("cashier").passwordHash(hashedPassword).build();
    Employee employee = TestDataBuilder.employee().user(user).build();
    Timesheet openTimesheet = TestDataBuilder.timesheet().employee(employee).checkOut(null).build();

    when(userDao.findByLogin("cashier")).thenReturn(Optional.of(user));
    when(employeeDao.findByUserId(user.getId())).thenReturn(Optional.of(employee));
    when(timesheetDao.findByEmployeeIdAndPeriod(
            eq(employee.getId()), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(openTimesheet));

    String token = authService.login(request);

    assertNotNull(token);
    verify(timesheetDao, never()).save(any(Timesheet.class));
  }

  @Test
  void login_shouldNotOpenTimesheetForGuestUser() {
    LoginRequest request = LoginRequest.builder().login("guest").password("password123").build();

    String hashedPassword = PasswordUtil.hash("password123");
    User user =
        TestDataBuilder.user()
            .login("guest")
            .passwordHash(hashedPassword)
            .role(UserRole.GUEST)
            .build();

    when(userDao.findByLogin("guest")).thenReturn(Optional.of(user));

    String token = authService.login(request);

    assertNotNull(token);
    verify(employeeDao, never()).findByUserId(any());
    verify(timesheetDao, never()).save(any(Timesheet.class));
  }

  @Test
  void logout_shouldCloseTimesheetForNonGuestUser() {
    UUID userId = UUID.randomUUID();
    UUID employeeId = UUID.randomUUID();
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Employee employee = TestDataBuilder.employee().id(employeeId).build();
    Timesheet openTimesheet =
        TestDataBuilder.timesheet()
            .employee(employee)
            .checkIn(LocalTime.of(9, 0))
            .checkOut(null)
            .build();

    when(employeeDao.findByUserId(userId)).thenReturn(Optional.of(employee));
    when(timesheetDao.findByEmployeeIdAndPeriod(
            eq(employeeId), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(openTimesheet));

    authService.logout(caller);

    ArgumentCaptor<Timesheet> timesheetCaptor = ArgumentCaptor.forClass(Timesheet.class);
    verify(timesheetDao).update(timesheetCaptor.capture());

    Timesheet updatedTimesheet = timesheetCaptor.getValue();
    assertNotNull(updatedTimesheet.getCheckOut());
    assertNotNull(updatedTimesheet.getHoursWorked());
    assertTrue(updatedTimesheet.getHoursWorked().compareTo(BigDecimal.ZERO) > 0);
  }

  @Test
  void logout_shouldDoNothingForGuestUser() {
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(UUID.randomUUID())
            .username("guest")
            .role(UserRole.GUEST)
            .build();

    authService.logout(caller);

    verify(employeeDao, never()).findByUserId(any());
    verify(timesheetDao, never()).update(any());
  }

  @Test
  void logout_shouldThrowExceptionIfEmployeeNotFound() {
    UUID userId = UUID.randomUUID();
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    when(employeeDao.findByUserId(userId)).thenReturn(Optional.empty());

    ServiceException exception =
        assertThrows(ServiceException.class, () -> authService.logout(caller));

    assertEquals("EMPLOYEE_NOT_FOUND", exception.getErrorCode());
    assertEquals("Сотрудник для пользователя не найден", exception.getMessage());
  }

  @Test
  void logout_shouldNotUpdateIfNoOpenTimesheet() {
    UUID userId = UUID.randomUUID();
    UUID employeeId = UUID.randomUUID();
    AuthenticatedUser caller =
        AuthenticatedUser.builder()
            .userId(userId)
            .username("cashier")
            .role(UserRole.CASHIER)
            .build();

    Employee employee = TestDataBuilder.employee().id(employeeId).build();
    Timesheet closedTimesheet =
        TestDataBuilder.timesheet()
            .employee(employee)
            .checkIn(LocalTime.of(9, 0))
            .checkOut(LocalTime.of(17, 0))
            .build();

    when(employeeDao.findByUserId(userId)).thenReturn(Optional.of(employee));
    when(timesheetDao.findByEmployeeIdAndPeriod(
            eq(employeeId), any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(closedTimesheet));

    authService.logout(caller);

    verify(timesheetDao, never()).update(any());
  }

  @Test
  void register_shouldCallUserServiceCreateGuest() {
    LoginRequest request = LoginRequest.builder().login("newuser").password("password123").build();

    authService.register(request);

    ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
    verify(userService).createGuest(captor.capture());

    CreateUserRequest capturedRequest = captor.getValue();
    assertEquals("newuser", capturedRequest.getLogin());
    assertEquals("password123", capturedRequest.getPassword());
  }

  @Test
  void register_shouldPropagateServiceException() {
    LoginRequest request =
        LoginRequest.builder().login("existinguser").password("password123").build();

    doThrow(new ServiceException("Пользователь уже существует", "USER_EXISTS"))
        .when(userService)
        .createGuest(any(CreateUserRequest.class));

    ServiceException exception =
        assertThrows(ServiceException.class, () -> authService.register(request));

    assertEquals("USER_EXISTS", exception.getErrorCode());
    assertEquals("Пользователь уже существует", exception.getMessage());
  }
}
