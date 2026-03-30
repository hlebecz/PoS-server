package kurs.backend.domain.persistence.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Sale;

public class SaleDao extends GenericDaoImpl<Sale, UUID> {

  public SaleDao() {
    super(Sale.class);
  }

  public List<Sale> findByStoreId(UUID storeId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Sale> result =
          session
              .createQuery("FROM Sale s WHERE s.store.id = :sid", Sale.class)
              .setParameter("sid", storeId)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<Sale> findByCashierId(UUID cashierId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Sale> result =
          session
              .createQuery("FROM Sale s WHERE s.cashier.id = :cid", Sale.class)
              .setParameter("cid", cashierId)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<Sale> findByStoreIdAndPeriod(UUID storeId, LocalDateTime from, LocalDateTime to) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Sale> result =
          session
              .createQuery(
                  """
                        FROM Sale s
                        WHERE s.store.id = :sid
                          AND s.soldAt BETWEEN :from AND :to
                        """,
                  Sale.class)
              .setParameter("sid", storeId)
              .setParameter("from", from)
              .setParameter("to", to)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
