package kurs.backend.domain.persistence.dao;

import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Product;

public class ProductDao extends GenericDaoImpl<Product, UUID> {

  public ProductDao() {
    super(Product.class);
  }

  public Optional<Product> findByArticle(String article) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      Optional<Product> result =
          session
              .createQuery("FROM Product p WHERE p.article = :article", Product.class)
              .setParameter("article", article)
              .uniqueResultOptional();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
