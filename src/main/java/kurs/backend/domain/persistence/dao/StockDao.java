package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Stock;
import kurs.backend.domain.persistence.entity.StockId;

public class StockDao extends GenericDaoImpl<Stock, StockId> {

  public StockDao() {
    super(Stock.class);
  }

  public List<Stock> findByStorageLocationId(UUID storageLocationId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Stock> result =
          session
              .createQuery("FROM Stock s WHERE s.storageLocation.id = :slid", Stock.class)
              .setParameter("slid", storageLocationId)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<Stock> findByProductId(UUID productId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Stock> result =
          session
              .createQuery("FROM Stock s WHERE s.product.id = :pid", Stock.class)
              .setParameter("pid", productId)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
