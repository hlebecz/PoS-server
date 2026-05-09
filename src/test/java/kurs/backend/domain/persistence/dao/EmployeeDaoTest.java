package kurs.backend.domain.persistence.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import kurs.backend.domain.persistence.entity.Location;
import kurs.backend.domain.persistence.entity.Store;
import kurs.backend.domain.persistence.entity.User;

class EmployeeDaoTest {

  private EmployeeDao employeeDao;
  private StoreDao storeDao;
  private UserDao userDao;
  private LocationDao locationDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = TestHibernateUtil.getSessionFactory();
    employeeDao = new EmployeeDao(sessionFactory);
    storeDao = new StoreDao(sessionFactory);
    userDao = new UserDao(sessionFactory);
    locationDao = new LocationDao(sessionFactory);
  }

  @AfterEach
  void tearDown() {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      session.createQuery("DELETE FROM Employee").executeUpdate();
      session.createQuery("DELETE FROM Store").executeUpdate();
      session.createQuery("DELETE FROM User").executeUpdate();
      session.createQuery("DELETE FROM Location").executeUpdate();
      tx.commit();
    } catch (Exception e) {
      tx.rollback();
      throw e;
    } finally {
      session.close();
    }
  }

  @Test
  void save_shouldPersistEmployee() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();

    Employee saved = employeeDao.save(employee);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(employee.getFullName(), saved.getFullName());
    assertEquals(employee.getPosition(), saved.getPosition());
    assertEquals(employee.getHourlyRate(), saved.getHourlyRate());
  }

  @Test
  void save_shouldPersistEmployeeWithUser() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    User user = TestDataBuilder.cashierUser().build();
    userDao.save(user);

    Employee employee = TestDataBuilder.employeeWithStore(store).user(user).build();

    Employee saved = employeeDao.save(employee);

    assertNotNull(saved);
    assertNotNull(saved.getUser());
    assertEquals(user.getId(), saved.getUser().getId());
  }

  @Test
  void save_shouldPersistEmployeeWithAllRelations() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    User user = TestDataBuilder.cashierUser().build();
    userDao.save(user);

    Location location = TestDataBuilder.location().build();
    locationDao.save(location);

    Employee employee = TestDataBuilder.employeeComplete(store, user, location).build();

    Employee saved = employeeDao.save(employee);

    assertNotNull(saved);
    assertNotNull(saved.getStore());
    assertNotNull(saved.getUser());
    assertNotNull(saved.getLocation());
    assertEquals(store.getId(), saved.getStore().getId());
    assertEquals(user.getId(), saved.getUser().getId());
    assertEquals(location.getId(), saved.getLocation().getId());
  }

  @Test
  void findById_shouldReturnEmployeeWhenExists() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    Employee saved = employeeDao.save(employee);

    Optional<Employee> found = employeeDao.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(saved.getFullName(), found.get().getFullName());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<Employee> found = employeeDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findByStoreId_shouldReturnEmployeesForStore() {
    Store store1 = TestDataBuilder.store();
    store1.setName("Store 1");
    storeDao.save(store1);

    Store store2 = TestDataBuilder.store();
    store2.setName("Store 2");
    storeDao.save(store2);

    Employee emp1 = TestDataBuilder.employeeWithStore(store1).fullName("Employee 1").build();
    Employee emp2 = TestDataBuilder.employeeWithStore(store1).fullName("Employee 2").build();
    Employee emp3 = TestDataBuilder.employeeWithStore(store2).fullName("Employee 3").build();

    employeeDao.save(emp1);
    employeeDao.save(emp2);
    employeeDao.save(emp3);

    List<Employee> store1Employees = employeeDao.findByStoreId(store1.getId());

    assertEquals(2, store1Employees.size());
    assertTrue(store1Employees.stream().allMatch(e -> e.getStore().getId().equals(store1.getId())));
  }

  @Test
  void findByUserId_shouldReturnEmployeeWhenExists() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    User user = TestDataBuilder.cashierUser().build();
    userDao.save(user);

    Employee employee = TestDataBuilder.employeeWithStore(store).user(user).build();
    employeeDao.save(employee);

    Optional<Employee> found = employeeDao.findByUserId(user.getId());

    assertTrue(found.isPresent());
    assertEquals(user.getId(), found.get().getUser().getId());
  }

  @Test
  void findByUserId_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentUserId = UUID.randomUUID();

    Optional<Employee> found = employeeDao.findByUserId(nonExistentUserId);

    assertFalse(found.isPresent());
  }

  @Test
  void findActive_shouldReturnOnlyActiveEmployees() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee active1 = TestDataBuilder.employeeWithStore(store).fullName("Active 1").build();
    Employee active2 = TestDataBuilder.employeeWithStore(store).fullName("Active 2").build();
    Employee fired =
        TestDataBuilder.employeeWithStore(store)
            .fullName("Fired")
            .firedAt(LocalDate.now().minusDays(10))
            .build();

    employeeDao.save(active1);
    employeeDao.save(active2);
    employeeDao.save(fired);

    List<Employee> activeEmployees = employeeDao.findActive();

    assertEquals(2, activeEmployees.size());
    assertTrue(activeEmployees.stream().allMatch(e -> e.getFiredAt() == null));
  }

  @Test
  void findAll_shouldReturnAllEmployees() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee emp1 = TestDataBuilder.employeeWithStore(store).fullName("Employee 1").build();
    Employee emp2 = TestDataBuilder.employeeWithStore(store).fullName("Employee 2").build();
    Employee emp3 = TestDataBuilder.employeeWithStore(store).fullName("Employee 3").build();

    employeeDao.save(emp1);
    employeeDao.save(emp2);
    employeeDao.save(emp3);

    List<Employee> allEmployees = employeeDao.findAll();

    assertEquals(3, allEmployees.size());
  }

  @Test
  void update_shouldModifyExistingEmployee() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).fullName("Original Name").build();
    Employee saved = employeeDao.save(employee);

    saved.setFullName("Updated Name");
    saved.setHourlyRate(BigDecimal.valueOf(30.00));
    Employee updated = employeeDao.update(saved);

    assertEquals("Updated Name", updated.getFullName());
    assertEquals(0, BigDecimal.valueOf(30.00).compareTo(updated.getHourlyRate()));

    Optional<Employee> found = employeeDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("Updated Name", found.get().getFullName());
  }

  @Test
  void delete_shouldRemoveEmployee() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    Employee saved = employeeDao.save(employee);

    employeeDao.delete(saved);

    Optional<Employee> found = employeeDao.findById(saved.getId());
    assertFalse(found.isPresent());
  }

  @Test
  void update_shouldAllowFiringEmployee() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee employee = TestDataBuilder.employeeWithStore(store).build();
    Employee saved = employeeDao.save(employee);

    saved.setFiredAt(LocalDate.now());
    employeeDao.update(saved);

    List<Employee> activeEmployees = employeeDao.findActive();
    assertFalse(activeEmployees.stream().anyMatch(e -> e.getId().equals(saved.getId())));
  }

  @Test
  void update_shouldAllowStoreChange() {
    Store store1 = TestDataBuilder.store();
    store1.setName("Store 1");
    storeDao.save(store1);

    Store store2 = TestDataBuilder.store();
    store2.setName("Store 2");
    storeDao.save(store2);

    Employee employee = TestDataBuilder.employeeWithStore(store1).build();
    Employee saved = employeeDao.save(employee);

    saved.setStore(store2);
    Employee updated = employeeDao.update(saved);

    assertEquals(store2.getId(), updated.getStore().getId());
  }
}
