package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Store;

public class StoreDao extends GenericDaoImpl<Store, UUID> {

  private static final Logger log = LogManager.getLogger(StoreDao.class);

  public StoreDao(SessionFactory sessionFactory) {
    super(Store.class, sessionFactory);
  }

  public List<Store> findAllActive() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findAllActive");
    try {
      List<Store> result =
          session
              .createQuery(
                  """
                  FROM Store s
                  LEFT JOIN FETCH s.manager
                  LEFT JOIN FETCH s.warehouse
                  LEFT JOIN FETCH s.location
                  WHERE s.isActive = true
                  """,
                  Store.class)
              .list();
      tx.commit();
      log.debug("Transaction committed: findAllActive - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findAllActive failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<Store> findByWarehouseId(UUID warehouseId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByWarehouseId with warehouseId={}", warehouseId);
    try {
      List<Store> result =
          session
              .createQuery(
                  """
                  FROM Store s
                  LEFT JOIN FETCH s.manager
                  LEFT JOIN FETCH s.warehouse
                  LEFT JOIN FETCH s.location
                  WHERE s.warehouse.id = :wid
                  """,
                  Store.class)
              .setParameter("wid", warehouseId)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByWarehouseId - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByWarehouseId failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<Store> findByManagerId(UUID managerId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByManagerId with managerId={}", managerId);
    try {
      List<Store> result =
          session
              .createQuery(
                  """
                  FROM Store s
                  LEFT JOIN FETCH s.manager
                  LEFT JOIN FETCH s.warehouse
                  LEFT JOIN FETCH s.location
                  WHERE s.manager.id = :mid
                  """,
                  Store.class)
              .setParameter("mid", managerId)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByManagerId - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByManagerId failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }
}
