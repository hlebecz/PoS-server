package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Stock;
import kurs.backend.domain.persistence.entity.StockId;

public class StockDao extends GenericDaoImpl<Stock, StockId> {

  private static final Logger log = LogManager.getLogger(StockDao.class);

  public StockDao(SessionFactory sessionFactory) {
    super(Stock.class, sessionFactory);
  }

  public List<Stock> findByStorageLocationId(UUID storageLocationId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug(
        "Starting transaction: findByStorageLocationId with storageLocationId={}",
        storageLocationId);
    try {
      List<Stock> result =
          session
              .createQuery(
                  """
                  FROM Stock s
                  LEFT JOIN FETCH s.storageLocation
                  LEFT JOIN FETCH s.product
                  WHERE s.storageLocation.id = :slid
                  """,
                  Stock.class)
              .setParameter("slid", storageLocationId)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByStorageLocationId - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByStorageLocationId failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<Stock> findByProductId(UUID productId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByProductId with productId={}", productId);
    try {
      List<Stock> result =
          session
              .createQuery(
                  """
                  FROM Stock s
                  LEFT JOIN FETCH s.storageLocation
                  LEFT JOIN FETCH s.product
                  WHERE s.product.id = :pid
                  """,
                  Stock.class)
              .setParameter("pid", productId)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByProductId - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByProductId failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }
}
