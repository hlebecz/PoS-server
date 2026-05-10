package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.graph.GraphSemantic;

public abstract class GenericDaoImpl<T, ID> implements GenericDao<T, ID> {

  private static final Logger log = LogManager.getLogger(GenericDaoImpl.class);

  private final Class<T> entityClass;
  private final SessionFactory sessionFactory;

  protected GenericDaoImpl(Class<T> entityClass, SessionFactory sessionFactory) {
    this.entityClass = entityClass;
    this.sessionFactory = sessionFactory;
  }

  protected Session getSession() {
    return sessionFactory.getCurrentSession();
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
    log.debug("Starting transaction: save {}", entityClass.getSimpleName());
    try {
      session.persist(entity);
      tx.commit();
      log.debug("Transaction committed: save {}", entityClass.getSimpleName());
      return entity;
    } catch (Exception e) {
      log.error(
          "Transaction rollback: save {} failed - {}", entityClass.getSimpleName(), e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  @Override
  public T update(T entity) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: update {}", entityClass.getSimpleName());
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
      log.debug("Transaction committed: update {}", entityClass.getSimpleName());
      return result;
    } catch (Exception e) {
      log.error(
          "Transaction rollback: update {} failed - {}",
          entityClass.getSimpleName(),
          e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  @Override
  public void delete(T entity) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: delete {}", entityClass.getSimpleName());
    try {
      session.remove(session.contains(entity) ? entity : session.merge(entity));
      tx.commit();
      log.debug("Transaction committed: delete {}", entityClass.getSimpleName());
    } catch (Exception e) {
      log.error(
          "Transaction rollback: delete {} failed - {}",
          entityClass.getSimpleName(),
          e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  @Override
  public Optional<T> findById(ID id) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findById {} with id={}", entityClass.getSimpleName(), id);
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
      log.debug(
          "Transaction committed: findById {} - found={}",
          entityClass.getSimpleName(),
          entity != null);
      return Optional.ofNullable(entity);
    } catch (Exception e) {
      log.error(
          "Transaction rollback: findById {} failed - {}",
          entityClass.getSimpleName(),
          e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  @Override
  public List<T> findAll() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findAll {}", entityClass.getSimpleName());
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
      log.debug(
          "Transaction committed: findAll {} - count={}",
          entityClass.getSimpleName(),
          result.size());
      return result;
    } catch (Exception e) {
      log.error(
          "Transaction rollback: findAll {} failed - {}",
          entityClass.getSimpleName(),
          e.getMessage());
      tx.rollback();
      throw e;
    }
  }
}
