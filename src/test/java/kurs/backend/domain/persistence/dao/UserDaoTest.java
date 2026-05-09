package kurs.backend.domain.persistence.dao;

import static org.junit.jupiter.api.Assertions.*;

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
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;

class UserDaoTest {

  private UserDao userDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    // Build test session factory
    sessionFactory = TestHibernateUtil.getSessionFactory();

    // Create the DAO with injected SessionFactory
    userDao = new UserDao(sessionFactory);

    System.out.println("Test setup complete, sessionFactory: " + sessionFactory);
  }

  @AfterEach
  void tearDown() {
    // Clean up database after each test
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      session.createQuery("DELETE FROM User").executeUpdate();
      tx.commit();
    } catch (Exception e) {
      tx.rollback();
      throw e;
    } finally {
      session.close();
    }
  }

  @Test
  void save_shouldPersistUser() {
    User user = TestDataBuilder.user().build();

    User saved = userDao.save(user);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(user.getLogin(), saved.getLogin());
    assertEquals(user.getRole(), saved.getRole());
    assertTrue(saved.getIsActive());
  }

  @Test
  void findById_shouldReturnUserWhenExists() {
    User user = TestDataBuilder.user().build();
    User saved = userDao.save(user);

    Optional<User> found = userDao.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(saved.getLogin(), found.get().getLogin());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<User> found = userDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findByLogin_shouldReturnUserWhenExists() {
    User user = TestDataBuilder.user().login("uniqueuser").build();
    userDao.save(user);

    Optional<User> found = userDao.findByLogin("uniqueuser");

    assertTrue(found.isPresent());
    assertEquals("uniqueuser", found.get().getLogin());
  }

  @Test
  void findByLogin_shouldReturnEmptyWhenNotExists() {
    Optional<User> found = userDao.findByLogin("nonexistent");

    assertFalse(found.isPresent());
  }

  @Test
  void findByRole_shouldReturnUsersWithSpecificRole() {
    User admin1 = TestDataBuilder.adminUser().login("admin1").build();
    User admin2 = TestDataBuilder.adminUser().login("admin2").build();
    User cashier = TestDataBuilder.cashierUser().login("cashier1").build();

    userDao.save(admin1);
    userDao.save(admin2);
    userDao.save(cashier);

    List<User> admins = userDao.findByRole(UserRole.ADMIN);

    assertEquals(2, admins.size());
    assertTrue(admins.stream().allMatch(u -> u.getRole() == UserRole.ADMIN));
  }

  @Test
  void findByRole_shouldReturnEmptyListWhenNoUsersWithRole() {
    User cashier = TestDataBuilder.cashierUser().build();
    userDao.save(cashier);

    List<User> managers = userDao.findByRole(UserRole.MANAGER);

    assertTrue(managers.isEmpty());
  }

  @Test
  void findAllActive_shouldReturnOnlyActiveUsers() {
    User activeUser1 = TestDataBuilder.user().login("active1").isActive(true).build();
    User activeUser2 = TestDataBuilder.user().login("active2").isActive(true).build();
    User inactiveUser = TestDataBuilder.user().login("inactive").isActive(false).build();

    userDao.save(activeUser1);
    userDao.save(activeUser2);
    userDao.save(inactiveUser);

    List<User> activeUsers = userDao.findAllActive();

    assertEquals(2, activeUsers.size());
    assertTrue(activeUsers.stream().allMatch(User::getIsActive));
  }

  @Test
  void findAll_shouldReturnAllUsers() {
    User user1 = TestDataBuilder.user().login("user1").build();
    User user2 = TestDataBuilder.user().login("user2").build();
    User user3 = TestDataBuilder.user().login("user3").build();

    userDao.save(user1);
    userDao.save(user2);
    userDao.save(user3);

    List<User> allUsers = userDao.findAll();

    assertEquals(3, allUsers.size());
  }

  @Test
  void update_shouldModifyExistingUser() {
    User user = TestDataBuilder.user().login("original").build();
    User saved = userDao.save(user);

    saved.setLogin("updated");
    saved.setRole(UserRole.MANAGER);
    User updated = userDao.update(saved);

    assertEquals("updated", updated.getLogin());
    assertEquals(UserRole.MANAGER, updated.getRole());

    Optional<User> found = userDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("updated", found.get().getLogin());
  }

  @Test
  void delete_shouldRemoveUser() {
    User user = TestDataBuilder.user().build();
    User saved = userDao.save(user);

    userDao.delete(saved);

    Optional<User> found = userDao.findById(saved.getId());
    assertFalse(found.isPresent());
  }

  @Test
  void save_shouldHandleMultipleRoles() {
    User admin = TestDataBuilder.adminUser().login("admin").build();
    User manager = TestDataBuilder.managerUser().login("manager").build();
    User cashier = TestDataBuilder.cashierUser().login("cashier").build();
    User accountant = TestDataBuilder.accountantUser().login("accountant").build();
    User guest = TestDataBuilder.user().login("guest").role(UserRole.GUEST).build();

    userDao.save(admin);
    userDao.save(manager);
    userDao.save(cashier);
    userDao.save(accountant);
    userDao.save(guest);

    List<User> allUsers = userDao.findAll();

    assertEquals(5, allUsers.size());
  }
}
