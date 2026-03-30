package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.HibernateUtil;

public abstract class GenericDaoImpl<T, ID> implements GenericDao<T, ID> {

  private final Class<T> entityClass;

  protected GenericDaoImpl(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  protected Session getSession() {
    return HibernateUtil.getSessionFactory().getCurrentSession();
  }

  @Override
  public T save(T entity) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      session.persist(entity);
      tx.commit();
      return entity;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  @Override
  public T update(T entity) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      T merged = session.merge(entity);
      tx.commit();
      return merged;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  @Override
  public void delete(T entity) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      session.remove(session.contains(entity) ? entity : session.merge(entity));
      tx.commit();
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  @Override
  public Optional<T> findById(ID id) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      T entity = session.find(entityClass, id);
      tx.commit();
      return Optional.ofNullable(entity);
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  @Override
  public List<T> findAll() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<T> result =
          session.createQuery("FROM " + entityClass.getSimpleName(), entityClass).list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
