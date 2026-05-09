package kurs.backend.domain.persistence.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kurs.backend.TestDataBuilder;
import kurs.backend.domain.persistence.TestHibernateUtil;
import kurs.backend.domain.persistence.entity.Employee;
import kurs.backend.domain.persistence.entity.Store;
import kurs.backend.domain.persistence.entity.Timesheet;

class TimesheetDaoTest {

  private TimesheetDao timesheetDao;
  private EmployeeDao employeeDao;
  private StoreDao storeDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = TestHibernateUtil.getSessionFactory();
    timesheetDao = new TimesheetDao(sessionFactory);
    employeeDao = new EmployeeDao(sessionFactory);
    storeDao = new StoreDao(sessionFactory);
  }

  @AfterEach
  void tearDown() {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      session.createQuery("DELETE FROM Timesheet").executeUpdate();
      session.createQuery("DELETE FROM Employee").executeUpdate();
      session.createQuery("DELETE FROM Store").executeUpdate();
      tx.commit();
    } catch (Exception e) {
      tx.rollback();
      throw e;
    } finally {
      session.close();
    }
  }

  @Test
  void save_shouldPersistTimesheet() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    Timesheet timesheet = TestDataBuilder.timesheetWithEmployee(employee).build();

    Timesheet saved = timesheetDao.save(timesheet);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(timesheet.getWorkDate(), saved.getWorkDate());
    assertEquals(timesheet.getCheckIn(), saved.getCheckIn());
    assertEquals(timesheet.getCheckOut(), saved.getCheckOut());
  }

  @Test
  void save_shouldPersistOpenTimesheet() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    Timesheet timesheet = TestDataBuilder.openTimesheet(employee).build();

    Timesheet saved = timesheetDao.save(timesheet);

    assertNotNull(saved);
    assertNull(saved.getCheckOut());
    assertNull(saved.getHoursWorked());
  }

  @Test
  void findById_shouldReturnTimesheetWhenExists() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    Timesheet timesheet = TestDataBuilder.timesheetWithEmployee(employee).build();
    Timesheet saved = timesheetDao.save(timesheet);

    Optional<Timesheet> found = timesheetDao.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<Timesheet> found = timesheetDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findByEmployeeId_shouldReturnTimesheetsForEmployee() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee1 = TestDataBuilder.employeeWithStore(store).fullName("Employee 1").build();
    Employee employee2 = TestDataBuilder.employeeWithStore(store).fullName("Employee 2").build();
    employeeDao.save(employee1);
    employeeDao.save(employee2);

    Timesheet ts1 = TestDataBuilder.timesheetForDate(employee1, LocalDate.now()).build();
    Timesheet ts2 =
        TestDataBuilder.timesheetForDate(employee1, LocalDate.now().minusDays(1)).build();
    Timesheet ts3 = TestDataBuilder.timesheetForDate(employee2, LocalDate.now()).build();

    timesheetDao.save(ts1);
    timesheetDao.save(ts2);
    timesheetDao.save(ts3);

    List<Timesheet> employee1Timesheets = timesheetDao.findByEmployeeId(employee1.getId());

    assertEquals(2, employee1Timesheets.size());
    assertTrue(
        employee1Timesheets.stream()
            .allMatch(t -> t.getEmployee().getId().equals(employee1.getId())));
  }

  @Test
  void findByEmployeeIdAndPeriod_shouldReturnTimesheetsInPeriod() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    LocalDate today = LocalDate.now();
    LocalDate yesterday = today.minusDays(1);
    LocalDate lastWeek = today.minusDays(7);
    LocalDate lastMonth = today.minusDays(30);

    Timesheet ts1 = TestDataBuilder.timesheetForDate(employee, today).build();
    Timesheet ts2 = TestDataBuilder.timesheetForDate(employee, yesterday).build();
    Timesheet ts3 = TestDataBuilder.timesheetForDate(employee, lastWeek).build();
    Timesheet ts4 = TestDataBuilder.timesheetForDate(employee, lastMonth).build();

    timesheetDao.save(ts1);
    timesheetDao.save(ts2);
    timesheetDao.save(ts3);
    timesheetDao.save(ts4);

    List<Timesheet> lastWeekTimesheets =
        timesheetDao.findByEmployeeIdAndPeriod(employee.getId(), lastWeek, today);

    assertEquals(3, lastWeekTimesheets.size());
    assertTrue(
        lastWeekTimesheets.stream()
            .allMatch(t -> !t.getWorkDate().isBefore(lastWeek) && !t.getWorkDate().isAfter(today)));
  }

  @Test
  void findByEmployeeIdAndPeriod_shouldReturnEmptyListWhenNoTimesheetsInPeriod() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    Timesheet timesheet =
        TestDataBuilder.timesheetForDate(employee, LocalDate.now().minusDays(30)).build();
    timesheetDao.save(timesheet);

    List<Timesheet> recentTimesheets =
        timesheetDao.findByEmployeeIdAndPeriod(
            employee.getId(), LocalDate.now().minusDays(7), LocalDate.now());

    assertTrue(recentTimesheets.isEmpty());
  }

  @Test
  void findAll_shouldReturnAllTimesheets() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    Timesheet ts1 = TestDataBuilder.timesheetForDate(employee, LocalDate.now()).build();
    Timesheet ts2 =
        TestDataBuilder.timesheetForDate(employee, LocalDate.now().minusDays(1)).build();
    Timesheet ts3 =
        TestDataBuilder.timesheetForDate(employee, LocalDate.now().minusDays(2)).build();

    timesheetDao.save(ts1);
    timesheetDao.save(ts2);
    timesheetDao.save(ts3);

    List<Timesheet> allTimesheets = timesheetDao.findAll();

    assertEquals(3, allTimesheets.size());
  }

  @Test
  void update_shouldModifyExistingTimesheet() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    Timesheet timesheet = TestDataBuilder.openTimesheet(employee).build();
    Timesheet saved = timesheetDao.save(timesheet);

    saved.setCheckOut(LocalTime.of(17, 30));
    saved.setHoursWorked(BigDecimal.valueOf(8.5));
    Timesheet updated = timesheetDao.update(saved);

    assertEquals(LocalTime.of(17, 30), updated.getCheckOut());
    assertEquals(0, BigDecimal.valueOf(8.5).compareTo(updated.getHoursWorked()));

    Optional<Timesheet> found = timesheetDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals(LocalTime.of(17, 30), found.get().getCheckOut());
  }

  @Test
  void delete_shouldRemoveTimesheet() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    Timesheet timesheet = TestDataBuilder.timesheetWithEmployee(employee).build();
    Timesheet saved = timesheetDao.save(timesheet);

    timesheetDao.delete(saved);

    Optional<Timesheet> found = timesheetDao.findById(saved.getId());
    assertFalse(found.isPresent());
  }

  @Test
  void update_shouldAllowClosingOpenTimesheet() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(employee);

    Timesheet timesheet =
        TestDataBuilder.openTimesheet(employee).checkIn(LocalTime.of(9, 0)).build();
    Timesheet saved = timesheetDao.save(timesheet);

    assertNull(saved.getCheckOut());

    saved.setCheckOut(LocalTime.of(17, 0));
    saved.setHoursWorked(BigDecimal.valueOf(8.0));
    Timesheet updated = timesheetDao.update(saved);

    assertNotNull(updated.getCheckOut());
    assertNotNull(updated.getHoursWorked());
  }
}
