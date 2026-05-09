package kurs.backend.domain.persistence;

import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import kurs.backend.domain.persistence.entity.*;

/** Test-specific Hibernate utility that configures H2 in-memory database for testing. */
public class TestHibernateUtil {

  private static SessionFactory sessionFactory;

  public static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      sessionFactory = buildSessionFactory();
    }
    return sessionFactory;
  }

  private static SessionFactory buildSessionFactory() {
    try {
      System.out.println("Building test SessionFactory with H2 database...");

      // Create configuration
      Properties settings = new Properties();
      settings.put(Environment.DRIVER, "org.h2.Driver");
      settings.put(Environment.URL, "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
      settings.put(Environment.DIALECT, "kurs.backend.domain.persistence.H2DialectCustom");
      settings.put(Environment.HBM2DDL_AUTO, "create-drop");
      settings.put(Environment.SHOW_SQL, "false");
      settings.put(Environment.FORMAT_SQL, "true");
      settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");

      StandardServiceRegistry registry =
          new StandardServiceRegistryBuilder().applySettings(settings).build();

      System.out.println("Adding entity classes...");
      MetadataSources sources = new MetadataSources(registry);
      sources.addAnnotatedClass(Employee.class);
      sources.addAnnotatedClass(Location.class);
      sources.addAnnotatedClass(Product.class);
      sources.addAnnotatedClass(Sale.class);
      sources.addAnnotatedClass(SaleItem.class);
      sources.addAnnotatedClass(Stock.class);
      sources.addAnnotatedClass(StorageLocation.class);
      sources.addAnnotatedClass(Store.class);
      sources.addAnnotatedClass(Timesheet.class);
      sources.addAnnotatedClass(User.class);
      sources.addAnnotatedClass(Warehouse.class);

      Metadata metadata = sources.buildMetadata();

      System.out.println("Building SessionFactory...");
      SessionFactory factory = metadata.buildSessionFactory();
      System.out.println("Test SessionFactory built successfully!");
      return factory;
    } catch (Exception e) {
      System.err.println("Error building test SessionFactory:");
      e.printStackTrace();
      throw new RuntimeException("There was an error building the test factory", e);
    }
  }

  public static void shutdown() {
    if (sessionFactory != null) {
      sessionFactory.close();
    }
  }
}
