package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.graph.GraphSemantic;

import kurs.backend.domain.persistence.HibernateUtil;

public abstract class GenericDaoImpl<T, ID> implements GenericDao<T, ID> {

  private final Class<T> entityClass;

  protected GenericDaoImpl(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  protected Session getSession() {
    return HibernateUtil.getSessionFactory().getCurrentSession();
  }

  /**
   * Get the name of the entity graph to use for eager fetching. Override this in subclasses to
   * specify a custom entity graph. Default is "{EntityName}.full"
   */
  protected String getEntityGraphName() {
    return entityClass.getSimpleName() + ".full";
  }

  /** Check if an entity graph exists for this entity. */
  private boolean hasEntityGraph(Session session) {
    try {
      session.getEntityGraph(getEntityGraphName());
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
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
      session.flush();

      // Get the ID to re-fetch the entity
      Object id = session.getIdentifier(merged);

      // Clear the entity from session to force a fresh fetch
      session.detach(merged);

      // Re-fetch with entity graph to eagerly load relationships (if graph exists)
      T result;
      if (hasEntityGraph(session)) {
        result =
            session.find(
                entityClass,
                (ID) id,
                Map.of(
                    GraphSemantic.FETCH.getJakartaHintName(),
                    session.getEntityGraph(getEntityGraphName())));
      } else {
        result = session.find(entityClass, (ID) id);
      }

      tx.commit();
      return result;
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
      T entity;
      if (hasEntityGraph(session)) {
        entity =
            session.find(
                entityClass,
                id,
                Map.of(
                    GraphSemantic.FETCH.getJakartaHintName(),
                    session.getEntityGraph(getEntityGraphName())));
      } else {
        entity = session.find(entityClass, id);
      }
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
      List<T> result;
      if (hasEntityGraph(session)) {
        result =
            session
                .createQuery("FROM " + entityClass.getSimpleName(), entityClass)
                .setHint(
                    GraphSemantic.FETCH.getJakartaHintName(),
                    session.getEntityGraph(getEntityGraphName()))
                .list();
      } else {
        result = session.createQuery("FROM " + entityClass.getSimpleName(), entityClass).list();
      }
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
