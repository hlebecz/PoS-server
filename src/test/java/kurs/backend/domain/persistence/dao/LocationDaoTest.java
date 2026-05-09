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
import kurs.backend.domain.persistence.entity.Location;

class LocationDaoTest {

  private LocationDao locationDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = TestHibernateUtil.getSessionFactory();
    locationDao = new LocationDao(sessionFactory);
  }

  @AfterEach
  void tearDown() {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
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
  void save_shouldPersistLocation() {
    Location location = TestDataBuilder.location().build();

    Location saved = locationDao.save(location);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(location.getAddress(), saved.getAddress());
  }

  @Test
  void findById_shouldReturnLocationWhenExists() {
    Location location = TestDataBuilder.location().build();
    Location saved = locationDao.save(location);

    Optional<Location> found = locationDao.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(saved.getAddress(), found.get().getAddress());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<Location> found = locationDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findAll_shouldReturnAllLocations() {
    Location location1 = TestDataBuilder.location().address("123 Main St").build();
    Location location2 = TestDataBuilder.location().address("456 Oak Ave").build();
    Location location3 = TestDataBuilder.location().address("789 Pine Rd").build();

    locationDao.save(location1);
    locationDao.save(location2);
    locationDao.save(location3);

    List<Location> allLocations = locationDao.findAll();

    assertEquals(3, allLocations.size());
  }

  @Test
  void update_shouldModifyExistingLocation() {
    Location location = TestDataBuilder.location().address("Old Address").build();
    Location saved = locationDao.save(location);

    saved.setAddress("New Address");
    Location updated = locationDao.update(saved);

    assertEquals("New Address", updated.getAddress());

    Optional<Location> found = locationDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("New Address", found.get().getAddress());
  }

  @Test
  void delete_shouldRemoveLocation() {
    Location location = TestDataBuilder.location().build();
    Location saved = locationDao.save(location);

    locationDao.delete(saved);

    Optional<Location> found = locationDao.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
