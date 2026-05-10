package kurs.backend.domain.persistence;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import kurs.backend.domain.persistence.entity.*;

/**
 * Helper class to provide a single implementation of a SessionFactory to the application. When our
 * application needs to perform a persistence operation it will obtain a singleton SessionFactory
 * from this Hibernate util class. Using this singleton SessionFactory it can obtain a Session and
 * Session is basically the interface between our application and Hibernate. It is what we use to
 * perform different persistence operations.
 */
public class HibernateUtil {

  private static final Logger log = LogManager.getLogger(HibernateUtil.class);
  private static SessionFactory sessionFactory;

  public static SessionFactory getSessionFactory() {
    // Lazy initialization of production session factory
    if (sessionFactory == null) {
      synchronized (HibernateUtil.class) {
        if (sessionFactory == null) {
          sessionFactory = buildSessionFactory();
        }
      }
    }
    return sessionFactory;
  }

  private static SessionFactory buildSessionFactory() {
    try {
      log.info("Building Hibernate SessionFactory...");
      /**
       * The configuration object will hold all of our Hibernate specific properties. So it's going
       * to know how we want Hibernate to perform. Another purpose of our configuration is to hold
       * all of the mapping information.
       */
      Configuration configuration = new Configuration();
      configuration.setProperty("hibernate.connection.username", System.getenv("POSTGRES_USER"));
      configuration.setProperty(
          "hibernate.connection.password", System.getenv("POSTGRES_PASSWORD"));
      configuration.addAnnotatedClass(Employee.class);
      configuration.addAnnotatedClass(Location.class);
      configuration.addAnnotatedClass(Product.class);
      configuration.addAnnotatedClass(Sale.class);
      configuration.addAnnotatedClass(SaleItem.class);
      configuration.addAnnotatedClass(Stock.class);
      configuration.addAnnotatedClass(StorageLocation.class);
      configuration.addAnnotatedClass(Store.class);
      configuration.addAnnotatedClass(Timesheet.class);
      configuration.addAnnotatedClass(User.class);
      configuration.addAnnotatedClass(Warehouse.class);

      /**
       * Need to pass in the configuration to the StandardServerRegistryBuilder() and then that
       * builder pattern invoke the build method and pass the ServiceRegistry into the
       * BuildSessionFactoryMethod and eventually we'll end up with a SessionFactory.
       */
      SessionFactory factory =
          configuration.buildSessionFactory(
              new StandardServiceRegistryBuilder()
                  .applySettings(configuration.getProperties())
                  .build());
      log.info("Hibernate SessionFactory built successfully");
      return factory;
    } catch (Exception e) {
      log.error("Error building Hibernate SessionFactory", e);
      throw new RuntimeException("There was an error building the factory", e);
    }
  }
}
