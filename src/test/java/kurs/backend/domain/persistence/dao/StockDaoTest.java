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
import kurs.backend.domain.persistence.entity.Product;
import kurs.backend.domain.persistence.entity.Stock;
import kurs.backend.domain.persistence.entity.StockId;
import kurs.backend.domain.persistence.entity.Store;
import kurs.backend.domain.persistence.entity.Warehouse;

class StockDaoTest {

  private StockDao stockDao;
  private ProductDao productDao;
  private StoreDao storeDao;
  private WarehouseDao warehouseDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = TestHibernateUtil.getSessionFactory();
    stockDao = new StockDao(sessionFactory);
    productDao = new ProductDao(sessionFactory);
    storeDao = new StoreDao(sessionFactory);
    warehouseDao = new WarehouseDao(sessionFactory);
  }

  @AfterEach
  void tearDown() {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      session.createQuery("DELETE FROM Stock").executeUpdate();
      session.createQuery("DELETE FROM Product").executeUpdate();
      session.createQuery("DELETE FROM Store").executeUpdate();
      session.createQuery("DELETE FROM Warehouse").executeUpdate();
      tx.commit();
    } catch (Exception e) {
      tx.rollback();
      throw e;
    } finally {
      session.close();
    }
  }

  @Test
  void save_shouldPersistStockInStore() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Product product = TestDataBuilder.product().build();
    productDao.save(product);

    Stock stock = TestDataBuilder.stockWithProduct(product, store).build();

    Stock saved = stockDao.save(stock);

    assertNotNull(saved);
    assertEquals(stock.getQuantity(), saved.getQuantity());
    assertEquals(product.getId(), saved.getProduct().getId());
    assertEquals(store.getId(), saved.getStorageLocation().getId());
  }

  @Test
  void save_shouldPersistStockInWarehouse() {
    Warehouse warehouse = TestDataBuilder.warehouse();
    warehouseDao.save(warehouse);

    Product product = TestDataBuilder.product().build();
    productDao.save(product);

    Stock stock = TestDataBuilder.stockWithProduct(product, warehouse).build();

    Stock saved = stockDao.save(stock);

    assertNotNull(saved);
    assertEquals(stock.getQuantity(), saved.getQuantity());
    assertEquals(product.getId(), saved.getProduct().getId());
    assertEquals(warehouse.getId(), saved.getStorageLocation().getId());
  }

  @Test
  void findById_shouldReturnStockWhenExists() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Product product = TestDataBuilder.product().build();
    productDao.save(product);

    Stock stock = TestDataBuilder.stockWithProduct(product, store).build();
    Stock saved = stockDao.save(stock);

    StockId stockId = new StockId(store.getId(), product.getId());
    Optional<Stock> found = stockDao.findById(stockId);

    assertTrue(found.isPresent());
    assertEquals(saved.getQuantity(), found.get().getQuantity());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    StockId nonExistentId = new StockId(UUID.randomUUID(), UUID.randomUUID());

    Optional<Stock> found = stockDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findByStorageLocationId_shouldReturnStocksForLocation() {
    Store store1 = TestDataBuilder.store();
    store1.setName("Store 1");
    storeDao.save(store1);

    Store store2 = TestDataBuilder.store();
    store2.setName("Store 2");
    storeDao.save(store2);

    Product product1 = TestDataBuilder.product().article("SKU-001").build();
    Product product2 = TestDataBuilder.product().article("SKU-002").build();
    Product product3 = TestDataBuilder.product().article("SKU-003").build();
    productDao.save(product1);
    productDao.save(product2);
    productDao.save(product3);

    Stock stock1 = TestDataBuilder.stockWithProduct(product1, store1).build();
    Stock stock2 = TestDataBuilder.stockWithProduct(product2, store1).build();
    Stock stock3 = TestDataBuilder.stockWithProduct(product3, store2).build();

    stockDao.save(stock1);
    stockDao.save(stock2);
    stockDao.save(stock3);

    List<Stock> store1Stocks = stockDao.findByStorageLocationId(store1.getId());

    assertEquals(2, store1Stocks.size());
    assertTrue(
        store1Stocks.stream().allMatch(s -> s.getStorageLocation().getId().equals(store1.getId())));
  }

  @Test
  void findByProductId_shouldReturnStocksForProduct() {
    Store store1 = TestDataBuilder.store();
    store1.setName("Store 1");
    storeDao.save(store1);

    Store store2 = TestDataBuilder.store();
    store2.setName("Store 2");
    storeDao.save(store2);

    Warehouse warehouse = TestDataBuilder.warehouse();
    warehouseDao.save(warehouse);

    Product product1 = TestDataBuilder.product().article("SKU-001").build();
    Product product2 = TestDataBuilder.product().article("SKU-002").build();
    productDao.save(product1);
    productDao.save(product2);

    Stock stock1 = TestDataBuilder.stockWithProduct(product1, store1).build();
    Stock stock2 = TestDataBuilder.stockWithProduct(product1, store2).build();
    Stock stock3 = TestDataBuilder.stockWithProduct(product1, warehouse).build();
    Stock stock4 = TestDataBuilder.stockWithProduct(product2, store1).build();

    stockDao.save(stock1);
    stockDao.save(stock2);
    stockDao.save(stock3);
    stockDao.save(stock4);

    List<Stock> product1Stocks = stockDao.findByProductId(product1.getId());

    assertEquals(3, product1Stocks.size());
    assertTrue(
        product1Stocks.stream().allMatch(s -> s.getProduct().getId().equals(product1.getId())));
  }

  @Test
  void findAll_shouldReturnAllStocks() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Product product1 = TestDataBuilder.product().article("SKU-001").build();
    Product product2 = TestDataBuilder.product().article("SKU-002").build();
    Product product3 = TestDataBuilder.product().article("SKU-003").build();
    productDao.save(product1);
    productDao.save(product2);
    productDao.save(product3);

    Stock stock1 = TestDataBuilder.stockWithProduct(product1, store).build();
    Stock stock2 = TestDataBuilder.stockWithProduct(product2, store).build();
    Stock stock3 = TestDataBuilder.stockWithProduct(product3, store).build();

    stockDao.save(stock1);
    stockDao.save(stock2);
    stockDao.save(stock3);

    List<Stock> allStocks = stockDao.findAll();

    assertEquals(3, allStocks.size());
  }

  @Test
  void update_shouldModifyExistingStock() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Product product = TestDataBuilder.product().build();
    productDao.save(product);

    Stock stock = TestDataBuilder.stockWithProduct(product, store).quantity(100).build();
    Stock saved = stockDao.save(stock);

    saved.setQuantity(150);
    Stock updated = stockDao.update(saved);

    assertEquals(150, updated.getQuantity());

    StockId stockId = new StockId(store.getId(), product.getId());
    Optional<Stock> found = stockDao.findById(stockId);
    assertTrue(found.isPresent());
    assertEquals(150, found.get().getQuantity());
  }

  @Test
  void delete_shouldRemoveStock() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Product product = TestDataBuilder.product().build();
    productDao.save(product);

    Stock stock = TestDataBuilder.stockWithProduct(product, store).build();
    Stock saved = stockDao.save(stock);

    stockDao.delete(saved);

    StockId stockId = new StockId(store.getId(), product.getId());
    Optional<Stock> found = stockDao.findById(stockId);
    assertFalse(found.isPresent());
  }

  @Test
  void update_shouldAllowQuantityDecrease() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Product product = TestDataBuilder.product().build();
    productDao.save(product);

    Stock stock = TestDataBuilder.stockWithProduct(product, store).quantity(100).build();
    Stock saved = stockDao.save(stock);

    saved.setQuantity(50);
    Stock updated = stockDao.update(saved);

    assertEquals(50, updated.getQuantity());
  }

  @Test
  void update_shouldAllowZeroQuantity() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Product product = TestDataBuilder.product().build();
    productDao.save(product);

    Stock stock = TestDataBuilder.stockWithProduct(product, store).quantity(100).build();
    Stock saved = stockDao.save(stock);

    saved.setQuantity(0);
    Stock updated = stockDao.update(saved);

    assertEquals(0, updated.getQuantity());
  }

  @Test
  void findByStorageLocationId_shouldWorkForBothStoresAndWarehouses() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Warehouse warehouse = TestDataBuilder.warehouse();
    warehouseDao.save(warehouse);

    Product product1 = TestDataBuilder.product().article("SKU-001").build();
    Product product2 = TestDataBuilder.product().article("SKU-002").build();
    productDao.save(product1);
    productDao.save(product2);

    Stock storeStock = TestDataBuilder.stockWithProduct(product1, store).build();
    Stock warehouseStock = TestDataBuilder.stockWithProduct(product2, warehouse).build();

    stockDao.save(storeStock);
    stockDao.save(warehouseStock);

    List<Stock> storeStocks = stockDao.findByStorageLocationId(store.getId());
    List<Stock> warehouseStocks = stockDao.findByStorageLocationId(warehouse.getId());

    assertEquals(1, storeStocks.size());
    assertEquals(1, warehouseStocks.size());
  }
}
