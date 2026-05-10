package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;

public class UserDao extends GenericDaoImpl<User, UUID> {

  private static final Logger log = LogManager.getLogger(UserDao.class);

  public UserDao(SessionFactory sessionFactory) {
    super(User.class, sessionFactory);
  }

  public Optional<User> findByLogin(String login) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByLogin with login={}", login);
    try {
      Optional<User> result =
          session
              .createQuery("FROM User u WHERE u.login = :login", User.class)
              .setParameter("login", login)
              .uniqueResultOptional();
      tx.commit();
      log.debug("Transaction committed: findByLogin - found={}", result.isPresent());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByLogin failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<User> findByRole(UserRole role) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByRole with role={}", role);
    try {
      List<User> result =
          session
              .createQuery("FROM User u WHERE u.role = :role", User.class)
              .setParameter("role", role)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByRole - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByRole failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<User> findAllActive() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findAllActive");
    try {
      List<User> result =
          session.createQuery("FROM User u WHERE u.isActive = true", User.class).list();
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
