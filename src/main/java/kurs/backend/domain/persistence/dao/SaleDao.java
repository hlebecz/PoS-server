package kurs.backend.domain.persistence.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Sale;

public class SaleDao extends GenericDaoImpl<Sale, UUID> {

  private static final Logger log = LogManager.getLogger(SaleDao.class);

  public SaleDao(SessionFactory sessionFactory) {
    super(Sale.class, sessionFactory);
  }

  public List<Sale> findByStoreId(UUID storeId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByStoreId with storeId={}", storeId);
    try {
      List<Sale> result =
          session
              .createQuery(
                  """
                  FROM Sale s
                  LEFT JOIN FETCH s.store
                  LEFT JOIN FETCH s.cashier
                  LEFT JOIN FETCH s.items i
                  LEFT JOIN FETCH i.product
                  WHERE s.store.id = :sid
                  """,
                  Sale.class)
              .setParameter("sid", storeId)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByStoreId - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByStoreId failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<Sale> findByCashierId(UUID cashierId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByCashierId with cashierId={}", cashierId);
    try {
      List<Sale> result =
          session
              .createQuery(
                  """
                  FROM Sale s
                  LEFT JOIN FETCH s.store
                  LEFT JOIN FETCH s.cashier
                  LEFT JOIN FETCH s.items i
                  LEFT JOIN FETCH i.product
                  WHERE s.cashier.id = :cid
                  """,
                  Sale.class)
              .setParameter("cid", cashierId)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByCashierId - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByCashierId failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<Sale> findByStoreIdAndPeriod(UUID storeId, LocalDateTime from, LocalDateTime to) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug(
        "Starting transaction: findByStoreIdAndPeriod with storeId={}, from={}, to={}",
        storeId,
        from,
        to);
    try {
      List<Sale> result =
          session
              .createQuery(
                  """
                        FROM Sale s
                        LEFT JOIN FETCH s.store
                        LEFT JOIN FETCH s.cashier
                        LEFT JOIN FETCH s.items i
                        LEFT JOIN FETCH i.product
                        WHERE s.store.id = :sid
                          AND s.soldAt BETWEEN :from AND :to
                        """,
                  Sale.class)
              .setParameter("sid", storeId)
              .setParameter("from", from)
              .setParameter("to", to)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByStoreIdAndPeriod - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByStoreIdAndPeriod failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }
}
