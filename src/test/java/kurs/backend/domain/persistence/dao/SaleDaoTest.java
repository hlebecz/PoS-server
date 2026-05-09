package kurs.backend.domain.persistence.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import kurs.backend.domain.persistence.entity.Sale;
import kurs.backend.domain.persistence.entity.Store;

class SaleDaoTest {

  private SaleDao saleDao;
  private StoreDao storeDao;
  private EmployeeDao employeeDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = TestHibernateUtil.getSessionFactory();
    saleDao = new SaleDao(sessionFactory);
    storeDao = new StoreDao(sessionFactory);
    employeeDao = new EmployeeDao(sessionFactory);
  }

  @AfterEach
  void tearDown() {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      session.createQuery("DELETE FROM Sale").executeUpdate();
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
  void save_shouldPersistSale() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(cashier);

    Sale sale = TestDataBuilder.saleComplete(store, cashier).build();

    Sale saved = saleDao.save(sale);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(sale.getTotal(), saved.getTotal());
    assertEquals(sale.getIsReturn(), saved.getIsReturn());
  }

  @Test
  void save_shouldPersistReturnSale() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(cashier);

    Sale sale = TestDataBuilder.returnSale().store(store).cashier(cashier).build();

    Sale saved = saleDao.save(sale);

    assertNotNull(saved);
    assertTrue(saved.getIsReturn());
    assertTrue(saved.getTotal().compareTo(BigDecimal.ZERO) < 0);
  }

  @Test
  void findById_shouldReturnSaleWhenExists() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(cashier);

    Sale sale = TestDataBuilder.saleComplete(store, cashier).build();
    Sale saved = saleDao.save(sale);

    Optional<Sale> found = saleDao.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<Sale> found = saleDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findByStoreId_shouldReturnSalesForStore() {
    Store store1 = TestDataBuilder.store();
    store1.setName("Store 1");
    storeDao.save(store1);

    Store store2 = TestDataBuilder.store();
    store2.setName("Store 2");
    storeDao.save(store2);

    Employee cashier1 = TestDataBuilder.employeeWithStore(store1).build();
    Employee cashier2 = TestDataBuilder.employeeWithStore(store2).build();
    employeeDao.save(cashier1);
    employeeDao.save(cashier2);

    Sale sale1 = TestDataBuilder.saleComplete(store1, cashier1).build();
    Sale sale2 = TestDataBuilder.saleComplete(store1, cashier1).build();
    Sale sale3 = TestDataBuilder.saleComplete(store2, cashier2).build();

    saleDao.save(sale1);
    saleDao.save(sale2);
    saleDao.save(sale3);

    List<Sale> store1Sales = saleDao.findByStoreId(store1.getId());

    assertEquals(2, store1Sales.size());
    assertTrue(store1Sales.stream().allMatch(s -> s.getStore().getId().equals(store1.getId())));
  }

  @Test
  void findByCashierId_shouldReturnSalesForCashier() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier1 = TestDataBuilder.employeeWithStore(store).fullName("Cashier 1").build();
    Employee cashier2 = TestDataBuilder.employeeWithStore(store).fullName("Cashier 2").build();
    employeeDao.save(cashier1);
    employeeDao.save(cashier2);

    Sale sale1 = TestDataBuilder.saleComplete(store, cashier1).build();
    Sale sale2 = TestDataBuilder.saleComplete(store, cashier1).build();
    Sale sale3 = TestDataBuilder.saleComplete(store, cashier2).build();

    saleDao.save(sale1);
    saleDao.save(sale2);
    saleDao.save(sale3);

    List<Sale> cashier1Sales = saleDao.findByCashierId(cashier1.getId());

    assertEquals(2, cashier1Sales.size());
    assertTrue(
        cashier1Sales.stream().allMatch(s -> s.getCashier().getId().equals(cashier1.getId())));
  }

    @Test
    void findByStoreIdAndPeriod_shouldReturnSalesInPeriod() {
      Store store = TestDataBuilder.store();
      storeDao.save(store);

      Employee cashier = TestDataBuilder.employeeWithStore(store).build();
      employeeDao.save(cashier);

      LocalDateTime baseTime = LocalDateTime.of(2026, 5, 7, 12, 0, 0);
      LocalDateTime yesterday = baseTime.minusDays(1);
      LocalDateTime lastWeek = baseTime.minusDays(7);
      LocalDateTime lastMonth = baseTime.minusDays(30);

      Sale sale1 = TestDataBuilder.saleComplete(store, cashier).soldAt(baseTime).build();
      Sale sale2 = TestDataBuilder.saleComplete(store, cashier).soldAt(yesterday).build();
      Sale sale3 = TestDataBuilder.saleComplete(store, cashier).soldAt(lastWeek).build();
      Sale sale4 = TestDataBuilder.saleComplete(store, cashier).soldAt(lastMonth).build();

      saleDao.save(sale1);
      saleDao.save(sale2);
      saleDao.save(sale3);
      saleDao.save(sale4);

      // Query with very wide time range
      List<Sale> allSalesInRange =
          saleDao.findByStoreIdAndPeriod(store.getId(), lastMonth.minusDays(1),
   baseTime.plusDays(1));

      // Should get all 4 sales
      assertEquals(4, allSalesInRange.size());
    }

  @Test
  void findByStoreIdAndPeriod_shouldReturnEmptyListWhenNoSalesInPeriod() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(cashier);

    LocalDateTime baseTime = LocalDateTime.of(2026, 5, 7, 12, 0, 0);
    Sale sale = TestDataBuilder.saleComplete(store, cashier).soldAt(baseTime.minusDays(30)).build();
    saleDao.save(sale);

    List<Sale> recentSales =
        saleDao.findByStoreIdAndPeriod(store.getId(), baseTime.minusDays(7), baseTime.plusHours(1));

    assertTrue(recentSales.isEmpty());
  }

  @Test
  void findAll_shouldReturnAllSales() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(cashier);

    Sale sale1 = TestDataBuilder.saleComplete(store, cashier).build();
    Sale sale2 = TestDataBuilder.saleComplete(store, cashier).build();
    Sale sale3 = TestDataBuilder.saleComplete(store, cashier).build();

    saleDao.save(sale1);
    saleDao.save(sale2);
    saleDao.save(sale3);

    List<Sale> allSales = saleDao.findAll();

    assertEquals(3, allSales.size());
  }

  @Test
  void update_shouldModifyExistingSale() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(cashier);

    Sale sale = TestDataBuilder.saleComplete(store, cashier).build();
    Sale saved = saleDao.save(sale);

    saved.setTotal(BigDecimal.valueOf(299.99));
    Sale updated = saleDao.update(saved);

    assertEquals(BigDecimal.valueOf(299.99), updated.getTotal());

    Optional<Sale> found = saleDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals(BigDecimal.valueOf(299.99), found.get().getTotal());
  }

  @Test
  void delete_shouldRemoveSale() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(cashier);

    Sale sale = TestDataBuilder.saleComplete(store, cashier).build();
    Sale saved = saleDao.save(sale);

    saleDao.delete(saved);

    Optional<Sale> found = saleDao.findById(saved.getId());
    assertFalse(found.isPresent());
  }

  @Test
  void findByStoreId_shouldIncludeBothSalesAndReturns() {
    Store store = TestDataBuilder.store();
    storeDao.save(store);

    Employee cashier = TestDataBuilder.employeeWithStore(store).build();
    employeeDao.save(cashier);

    Sale normalSale = TestDataBuilder.saleComplete(store, cashier).isReturn(false).build();
    Sale returnSale = TestDataBuilder.returnSale().store(store).cashier(cashier).build();

    saleDao.save(normalSale);
    saleDao.save(returnSale);

    List<Sale> allSales = saleDao.findByStoreId(store.getId());

    assertEquals(2, allSales.size());
    assertEquals(1, allSales.stream().filter(Sale::getIsReturn).count());
    assertEquals(1, allSales.stream().filter(s -> !s.getIsReturn()).count());
  }
}
