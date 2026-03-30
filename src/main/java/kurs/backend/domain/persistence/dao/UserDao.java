package kurs.backend.domain.persistence.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.UserRole;

public class UserDao extends GenericDaoImpl<User, UUID> {

  public UserDao() {
    super(User.class);
  }

  public Optional<User> findByLogin(String login) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      Optional<User> result =
          session
              .createQuery("FROM User u WHERE u.login = :login", User.class)
              .setParameter("login", login)
              .uniqueResultOptional();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<User> findByRole(UserRole role) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<User> result =
          session
              .createQuery("FROM User u WHERE u.role = :role", User.class)
              .setParameter("role", role)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<User> findAllActive() {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<User> result =
          session.createQuery("FROM User u WHERE u.isActive = true", User.class).list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
