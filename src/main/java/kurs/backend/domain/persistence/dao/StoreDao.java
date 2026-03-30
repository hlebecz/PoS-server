package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Store;

public class StoreDao extends GenericDaoImpl<Store, UUID> {

  public StoreDao() {
    super(Store.class);
  }

  public List<Store> findAllActive() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Store> result =
          session.createQuery("FROM Store s WHERE s.isActive = true", Store.class).list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<Store> findByWarehouseId(UUID warehouseId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Store> result =
          session
              .createQuery("FROM Store s WHERE s.warehouse.id = :wid", Store.class)
              .setParameter("wid", warehouseId)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<Store> findByManagerId(UUID managerId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Store> result =
          session
              .createQuery("FROM Store s WHERE s.manager.id = :mid", Store.class)
              .setParameter("mid", managerId)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
