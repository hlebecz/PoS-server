package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Employee;

public class EmployeeDao extends GenericDaoImpl<Employee, UUID> {

  public EmployeeDao() {
    super(Employee.class);
  }

  public List<Employee> findByStoreId(UUID storeId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Employee> result =
          session
              .createQuery("FROM Employee e WHERE e.store.id = :sid", Employee.class)
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
              .createQuery("FROM Employee e WHERE e.user.id = :uid", Employee.class)
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
          session.createQuery("FROM Employee e WHERE e.firedAt IS NULL", Employee.class).list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
