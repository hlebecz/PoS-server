package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Warehouse;

public class WarehouseDao extends GenericDaoImpl<Warehouse, UUID> {

  private static final Logger log = LogManager.getLogger(WarehouseDao.class);

  public WarehouseDao(SessionFactory sessionFactory) {
    super(Warehouse.class, sessionFactory);
  }

  public List<Warehouse> findAllActive() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findAllActive");
    try {
      List<Warehouse> result =
          session
              .createQuery(
                  """
                  FROM Warehouse w
                  LEFT JOIN FETCH w.location
                  WHERE w.isActive = true
                  """,
                  Warehouse.class)
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
}
