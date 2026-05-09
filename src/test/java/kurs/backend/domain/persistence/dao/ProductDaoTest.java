package kurs.backend.domain.persistence.dao;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
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

class ProductDaoTest {

  private ProductDao productDao;
  private SessionFactory sessionFactory;

  @BeforeEach
  void setUp() {
    sessionFactory = TestHibernateUtil.getSessionFactory();
    productDao = new ProductDao(sessionFactory);
  }

  @AfterEach
  void tearDown() {
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
    try {
      session.createQuery("DELETE FROM Product").executeUpdate();
      tx.commit();
    } catch (Exception e) {
      tx.rollback();
      throw e;
    } finally {
      session.close();
    }
  }

  @Test
  void save_shouldPersistProduct() {
    Product product = TestDataBuilder.product().build();

    Product saved = productDao.save(product);

    assertNotNull(saved);
    assertNotNull(saved.getId());
    assertEquals(product.getName(), saved.getName());
    assertEquals(product.getArticle(), saved.getArticle());
    assertEquals(product.getPrice(), saved.getPrice());
  }

  @Test
  void findById_shouldReturnProductWhenExists() {
    Product product = TestDataBuilder.product().build();
    Product saved = productDao.save(product);

    Optional<Product> found = productDao.findById(saved.getId());

    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals(saved.getName(), found.get().getName());
  }

  @Test
  void findById_shouldReturnEmptyWhenNotExists() {
    UUID nonExistentId = UUID.randomUUID();

    Optional<Product> found = productDao.findById(nonExistentId);

    assertFalse(found.isPresent());
  }

  @Test
  void findByArticle_shouldReturnProductWhenExists() {
    Product product = TestDataBuilder.product().article("UNIQUE-SKU-123").build();
    productDao.save(product);

    Optional<Product> found = productDao.findByArticle("UNIQUE-SKU-123");

    assertTrue(found.isPresent());
    assertEquals("UNIQUE-SKU-123", found.get().getArticle());
  }

  @Test
  void findByArticle_shouldReturnEmptyWhenNotExists() {
    Optional<Product> found = productDao.findByArticle("NON-EXISTENT");

    assertFalse(found.isPresent());
  }

  @Test
  void findAll_shouldReturnAllProducts() {
    Product product1 = TestDataBuilder.product().article("SKU-001").build();
    Product product2 = TestDataBuilder.product().article("SKU-002").build();
    Product product3 = TestDataBuilder.product().article("SKU-003").build();

    productDao.save(product1);
    productDao.save(product2);
    productDao.save(product3);

    List<Product> allProducts = productDao.findAll();

    assertEquals(3, allProducts.size());
  }

  @Test
  void update_shouldModifyExistingProduct() {
    Product product = TestDataBuilder.product().name("Original Name").build();
    Product saved = productDao.save(product);

    saved.setName("Updated Name");
    saved.setPrice(BigDecimal.valueOf(149.99));
    Product updated = productDao.update(saved);

    assertEquals("Updated Name", updated.getName());
    assertEquals(BigDecimal.valueOf(149.99), updated.getPrice());

    Optional<Product> found = productDao.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("Updated Name", found.get().getName());
  }

  @Test
  void delete_shouldRemoveProduct() {
    Product product = TestDataBuilder.product().build();
    Product saved = productDao.save(product);

    productDao.delete(saved);

    Optional<Product> found = productDao.findById(saved.getId());
    assertFalse(found.isPresent());
  }

  @Test
  void save_shouldHandleDifferentPrices() {
    Product cheap =
        TestDataBuilder.product().article("CHEAP").price(BigDecimal.valueOf(9.99)).build();
    Product expensive =
        TestDataBuilder.product().article("EXPENSIVE").price(BigDecimal.valueOf(999.99)).build();

    productDao.save(cheap);
    productDao.save(expensive);

    Optional<Product> foundCheap = productDao.findByArticle("CHEAP");
    Optional<Product> foundExpensive = productDao.findByArticle("EXPENSIVE");

    assertTrue(foundCheap.isPresent());
    assertTrue(foundExpensive.isPresent());
    assertEquals(0, foundCheap.get().getPrice().compareTo(BigDecimal.valueOf(9.99)));
    assertEquals(0, foundExpensive.get().getPrice().compareTo(BigDecimal.valueOf(999.99)));
  }

  @Test
  void update_shouldAllowArticleChange() {
    Product product = TestDataBuilder.product().article("OLD-SKU").build();
    Product saved = productDao.save(product);

    saved.setArticle("NEW-SKU");
    Product updated = productDao.update(saved);

    assertEquals("NEW-SKU", updated.getArticle());
    assertFalse(productDao.findByArticle("OLD-SKU").isPresent());
    assertTrue(productDao.findByArticle("NEW-SKU").isPresent());
  }
}
