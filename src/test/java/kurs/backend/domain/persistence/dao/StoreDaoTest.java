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
import kurs.backend.domain.persistence.entity.Store;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.Warehouse;

class StoreDaoTest {

  private StoreDao storeDao;
  private UserDao userDao;
  private WarehouseDao warehouseDao;
  private LocationDao locationDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = TestHibernateUtil.getSessionFactory();
    storeDao = new StoreDao(sessionFactory);
    userDao = new UserDao(sessionFactory);
    warehouseDao = new WarehouseDao(sessionFactory);
    locationDao = new LocationDao(sessionFactory);
  }

  @AfterEach
  void tearDown() {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      session.createQuery("DELETE FROM Store").executeUpdate();
      session.createQuery("DELETE FROM Warehouse").executeUpdate();
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
  void save_shouldPersistStore() {
    Store store = TestDataBuilder.store();

    Store saved = storeDao.save(store);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(store.getName(), saved.getName());
    assertEquals(store.getPhone(), saved.getPhone());
    assertTrue(saved.getIsActive());
  }

  @Test
  void save_shouldPersistStoreWithManager() {
    User manager = TestDataBuilder.managerUser().build();
    userDao.save(manager);

    Store store = TestDataBuilder.storeWithManager(manager);
    Store saved = storeDao.save(store);

    assertNotNull(saved);
    assertNotNull(saved.getManager());
    assertEquals(manager.getId(), saved.getManager().getId());
  }

  @Test
  void save_shouldPersistStoreWithWarehouse() {
    Warehouse warehouse = TestDataBuilder.warehouse();
    warehouseDao.save(warehouse);

    Store store = TestDataBuilder.storeWithWarehouse(warehouse);
    Store saved = storeDao.save(store);

    assertNotNull(saved);
    assertNotNull(saved.getWarehouse());
    assertEquals(warehouse.getId(), saved.getWarehouse().getId());
  }

  @Test
  void save_shouldPersistStoreWithAllRelations() {
    User manager = TestDataBuilder.managerUser().build();
    userDao.save(manager);

    Warehouse warehouse = TestDataBuilder.warehouse();
    warehouseDao.save(warehouse);

    Location location = TestDataBuilder.location().build();
    locationDao.save(location);

    Store store = TestDataBuilder.storeComplete(manager, warehouse, location);
    Store saved = storeDao.save(store);

    assertNotNull(saved);
    assertNotNull(saved.getManager());
    assertNotNull(saved.getWarehouse());
    assertNotNull(saved.getLocation());
    assertEquals(manager.getId(), saved.getManager().getId());
    assertEquals(warehouse.getId(), saved.getWarehouse().getId());
    assertEquals(location.getId(), saved.getLocation().getId());
  }

  @Test
  void findById_shouldReturnStoreWhenExists() {
    Store store = TestDataBuilder.store();
    Store saved = storeDao.save(store);

    Optional<Store> found = storeDao.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(saved.getName(), found.get().getName());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<Store> found = storeDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findAllActive_shouldReturnOnlyActiveStores() {
    Store active1 = TestDataBuilder.store();
    active1.setName("Active 1");
    active1.setIsActive(true);

    Store active2 = TestDataBuilder.store();
    active2.setName("Active 2");
    active2.setIsActive(true);

    Store inactive = TestDataBuilder.store();
    inactive.setName("Inactive");
    inactive.setIsActive(false);

    storeDao.save(active1);
    storeDao.save(active2);
    storeDao.save(inactive);

    List<Store> activeStores = storeDao.findAllActive();

    assertEquals(2, activeStores.size());
    assertTrue(activeStores.stream().allMatch(Store::getIsActive));
  }

  @Test
  void findByWarehouseId_shouldReturnStoresForWarehouse() {
    Warehouse warehouse1 = TestDataBuilder.warehouse();
    warehouse1.setName("Warehouse 1");
    warehouseDao.save(warehouse1);

    Warehouse warehouse2 = TestDataBuilder.warehouse();
    warehouse2.setName("Warehouse 2");
    warehouseDao.save(warehouse2);

    Store store1 = TestDataBuilder.storeWithWarehouse(warehouse1);
    store1.setName("Store 1");
    Store store2 = TestDataBuilder.storeWithWarehouse(warehouse1);
    store2.setName("Store 2");
    Store store3 = TestDataBuilder.storeWithWarehouse(warehouse2);
    store3.setName("Store 3");

    storeDao.save(store1);
    storeDao.save(store2);
    storeDao.save(store3);

    List<Store> warehouse1Stores = storeDao.findByWarehouseId(warehouse1.getId());

    assertEquals(2, warehouse1Stores.size());
    assertTrue(
        warehouse1Stores.stream()
            .allMatch(s -> s.getWarehouse().getId().equals(warehouse1.getId())));
  }

  @Test
  void findByManagerId_shouldReturnStoresForManager() {
    User manager1 = TestDataBuilder.managerUser().login("manager1").build();
    User manager2 = TestDataBuilder.managerUser().login("manager2").build();
    userDao.save(manager1);
    userDao.save(manager2);

    Store store1 = TestDataBuilder.storeWithManager(manager1);
    store1.setName("Store 1");
    Store store2 = TestDataBuilder.storeWithManager(manager1);
    store2.setName("Store 2");
    Store store3 = TestDataBuilder.storeWithManager(manager2);
    store3.setName("Store 3");

    storeDao.save(store1);
    storeDao.save(store2);
    storeDao.save(store3);

    List<Store> manager1Stores = storeDao.findByManagerId(manager1.getId());

    assertEquals(2, manager1Stores.size());
    assertTrue(
        manager1Stores.stream().allMatch(s -> s.getManager().getId().equals(manager1.getId())));
  }

  @Test
  void findAll_shouldReturnAllStores() {
    Store store1 = TestDataBuilder.store();
    store1.setName("Store 1");
    Store store2 = TestDataBuilder.store();
    store2.setName("Store 2");
    Store store3 = TestDataBuilder.store();
    store3.setName("Store 3");

    storeDao.save(store1);
    storeDao.save(store2);
    storeDao.save(store3);

    List<Store> allStores = storeDao.findAll();

    assertEquals(3, allStores.size());
  }

  @Test
  void update_shouldModifyExistingStore() {
    Store store = TestDataBuilder.store();
    store.setName("Original Name");
    Store saved = storeDao.save(store);

    saved.setName("Updated Name");
    saved.setPhone("555-9999");
    Store updated = storeDao.update(saved);

    assertEquals("Updated Name", updated.getName());
    assertEquals("555-9999", updated.getPhone());

    Optional<Store> found = storeDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("Updated Name", found.get().getName());
  }

  @Test
  void delete_shouldRemoveStore() {
    Store store = TestDataBuilder.store();
    Store saved = storeDao.save(store);

    storeDao.delete(saved);

    Optional<Store> found = storeDao.findById(saved.getId());
    assertFalse(found.isPresent());
  }

  @Test
  void update_shouldAllowManagerChange() {
    User manager1 = TestDataBuilder.managerUser().login("manager1").build();
    User manager2 = TestDataBuilder.managerUser().login("manager2").build();
    userDao.save(manager1);
    userDao.save(manager2);

    Store store = TestDataBuilder.storeWithManager(manager1);
    Store saved = storeDao.save(store);

    saved.setManager(manager2);
    Store updated = storeDao.update(saved);

    assertEquals(manager2.getId(), updated.getManager().getId());
  }

  @Test
  void update_shouldAllowDeactivation() {
    Store store = TestDataBuilder.store();
    store.setIsActive(true);
    Store saved = storeDao.save(store);

    saved.setIsActive(false);
    storeDao.update(saved);

    List<Store> activeStores = storeDao.findAllActive();
    assertFalse(activeStores.stream().anyMatch(s -> s.getId().equals(saved.getId())));
  }
}
