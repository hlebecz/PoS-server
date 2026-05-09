package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Employee;

public class EmployeeDao extends GenericDaoImpl<Employee, UUID> {

  public EmployeeDao(SessionFactory sessionFactory) {
    super(Employee.class, sessionFactory);
  }

  public List<Employee> findByStoreId(UUID storeId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
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
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public Optional<Employee> findByUserId(UUID userId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
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
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<Employee> findActive() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
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
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
