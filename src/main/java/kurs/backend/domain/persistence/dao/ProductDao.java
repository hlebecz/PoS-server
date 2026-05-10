package kurs.backend.domain.persistence.dao;

import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Product;

public class ProductDao extends GenericDaoImpl<Product, UUID> {

  private static final Logger log = LogManager.getLogger(ProductDao.class);

  public ProductDao(SessionFactory sessionFactory) {
    super(Product.class, sessionFactory);
  }

  public Optional<Product> findByArticle(String article) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByArticle with article={}", article);
    try {
      Optional<Product> result =
          session
              .createQuery("FROM Product p WHERE p.article = :article", Product.class)
              .setParameter("article", article)
              .uniqueResultOptional();
      tx.commit();
      log.debug("Transaction committed: findByArticle - found={}", result.isPresent());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByArticle failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }
}
