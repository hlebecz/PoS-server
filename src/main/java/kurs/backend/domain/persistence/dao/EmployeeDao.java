package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Employee;

public class EmployeeDao extends GenericDaoImpl<Employee, UUID> {

  private static final Logger log = LogManager.getLogger(EmployeeDao.class);

  public EmployeeDao(SessionFactory sessionFactory) {
    super(Employee.class, sessionFactory);
  }

  public List<Employee> findByStoreId(UUID storeId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByStoreId with storeId={}", storeId);
    try {
      List<Employee> result =
          session
              .createQuery(
                  """
                  FROM Employee e
                  LEFT JOIN FETCH e.user
                  LEFT JOIN FETCH e.store
                  LEFT JOIN FETCH e.location
                  WHERE e.store.id = :sid
                  """,
                  Employee.class)
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

  public Optional<Employee> findByUserId(UUID userId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByUserId with userId={}", userId);
    try {
      Optional<Employee> result =
          session
              .createQuery(
                  """
                  FROM Employee e
                  LEFT JOIN FETCH e.user
                  LEFT JOIN FETCH e.store
                  LEFT JOIN FETCH e.location
                  WHERE e.user.id = :uid
                  """,
                  Employee.class)
              .setParameter("uid", userId)
              .uniqueResultOptional();
      tx.commit();
      log.debug("Transaction committed: findByUserId - found={}", result.isPresent());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByUserId failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<Employee> findActive() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findActive");
    try {
      List<Employee> result =
          session
              .createQuery(
                  """
                  FROM Employee e
                  LEFT JOIN FETCH e.user
                  LEFT JOIN FETCH e.store
                  LEFT JOIN FETCH e.location
                  WHERE e.firedAt IS NULL
                  """,
                  Employee.class)
              .list();
      tx.commit();
      log.debug("Transaction committed: findActive - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findActive failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }
}
