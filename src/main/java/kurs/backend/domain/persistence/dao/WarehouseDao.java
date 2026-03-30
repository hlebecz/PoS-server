package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Warehouse;

public class WarehouseDao extends GenericDaoImpl<Warehouse, UUID> {

  public WarehouseDao() {
    super(Warehouse.class);
  }

  public List<Warehouse> findAllActive() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Warehouse> result =
          session.createQuery("FROM Warehouse w WHERE w.isActive = true", Warehouse.class).list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
