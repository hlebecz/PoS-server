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
import kurs.backend.domain.persistence.entity.Warehouse;

class WarehouseDaoTest {

  private WarehouseDao warehouseDao;
  private LocationDao locationDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = TestHibernateUtil.getSessionFactory();
    warehouseDao = new WarehouseDao(sessionFactory);
    locationDao = new LocationDao(sessionFactory);
  }

  @AfterEach
  void tearDown() {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      session.createQuery("DELETE FROM Warehouse").executeUpdate();
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
  void save_shouldPersistWarehouse() {
    Warehouse warehouse = TestDataBuilder.warehouse();

    Warehouse saved = warehouseDao.save(warehouse);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(warehouse.getName(), saved.getName());
    assertEquals(warehouse.getPhone(), saved.getPhone());
    assertTrue(saved.getIsActive());
  }

  @Test
  void save_shouldPersistWarehouseWithLocation() {
    Location location = TestDataBuilder.location().build();
    locationDao.save(location);

    Warehouse warehouse = TestDataBuilder.warehouseWithLocation(location);
    Warehouse saved = warehouseDao.save(warehouse);

    assertNotNull(saved);
    assertNotNull(saved.getLocation());
    assertEquals(location.getId(), saved.getLocation().getId());
  }

  @Test
  void findById_shouldReturnWarehouseWhenExists() {
    Warehouse warehouse = TestDataBuilder.warehouse();
    Warehouse saved = warehouseDao.save(warehouse);

    Optional<Warehouse> found = warehouseDao.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(saved.getName(), found.get().getName());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<Warehouse> found = warehouseDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findAllActive_shouldReturnOnlyActiveWarehouses() {
    Warehouse active1 = TestDataBuilder.warehouse();
    active1.setName("Active 1");
    active1.setIsActive(true);

    Warehouse active2 = TestDataBuilder.warehouse();
    active2.setName("Active 2");
    active2.setIsActive(true);

    Warehouse inactive = TestDataBuilder.warehouse();
    inactive.setName("Inactive");
    inactive.setIsActive(false);

    warehouseDao.save(active1);
    warehouseDao.save(active2);
    warehouseDao.save(inactive);

    List<Warehouse> activeWarehouses = warehouseDao.findAllActive();

    assertEquals(2, activeWarehouses.size());
    assertTrue(activeWarehouses.stream().allMatch(Warehouse::getIsActive));
  }

  @Test
  void findAll_shouldReturnAllWarehouses() {
    Warehouse warehouse1 = TestDataBuilder.warehouse();
    warehouse1.setName("Warehouse 1");

    Warehouse warehouse2 = TestDataBuilder.warehouse();
    warehouse2.setName("Warehouse 2");

    Warehouse warehouse3 = TestDataBuilder.warehouse();
    warehouse3.setName("Warehouse 3");

    warehouseDao.save(warehouse1);
    warehouseDao.save(warehouse2);
    warehouseDao.save(warehouse3);

    List<Warehouse> allWarehouses = warehouseDao.findAll();

    assertEquals(3, allWarehouses.size());
  }

  @Test
  void update_shouldModifyExistingWarehouse() {
    Warehouse warehouse = TestDataBuilder.warehouse();
    warehouse.setName("Original Name");
    Warehouse saved = warehouseDao.save(warehouse);

    saved.setName("Updated Name");
    saved.setPhone("555-9999");
    Warehouse updated = warehouseDao.update(saved);

    assertEquals("Updated Name", updated.getName());
    assertEquals("555-9999", updated.getPhone());

    Optional<Warehouse> found = warehouseDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("Updated Name", found.get().getName());
  }

  @Test
  void delete_shouldRemoveWarehouse() {
    Warehouse warehouse = TestDataBuilder.warehouse();
    Warehouse saved = warehouseDao.save(warehouse);

    warehouseDao.delete(saved);

    Optional<Warehouse> found = warehouseDao.findById(saved.getId());
    assertFalse(found.isPresent());
  }

  @Test
  void update_shouldAllowDeactivation() {
    Warehouse warehouse = TestDataBuilder.warehouse();
    warehouse.setIsActive(true);
    Warehouse saved = warehouseDao.save(warehouse);

    saved.setIsActive(false);
    warehouseDao.update(saved);

    List<Warehouse> activeWarehouses = warehouseDao.findAllActive();
    assertFalse(activeWarehouses.stream().anyMatch(w -> w.getId().equals(saved.getId())));
  }

  @Test
  void update_shouldAllowLocationChange() {
    Location location1 = TestDataBuilder.location().address("Location 1").build();
    Location location2 = TestDataBuilder.location().address("Location 2").build();
    locationDao.save(location1);
    locationDao.save(location2);

    Warehouse warehouse = TestDataBuilder.warehouseWithLocation(location1);
    Warehouse saved = warehouseDao.save(warehouse);

    saved.setLocation(location2);
    Warehouse updated = warehouseDao.update(saved);

    assertEquals(location2.getId(), updated.getLocation().getId());
  }
}
