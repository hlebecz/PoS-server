package kurs.backend.domain.persistence;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

/** Custom H2 dialect that handles PostgreSQL-specific types for testing. */
public class H2DialectCustom extends H2Dialect {

  public H2DialectCustom() {
    super();
  }

  @Override
  public void contributeTypes(
      org.hibernate.boot.model.TypeContributions typeContributions,
      org.hibernate.service.ServiceRegistry serviceRegistry) {
    super.contributeTypes(typeContributions, serviceRegistry);

    // Register custom type mappings for PostgreSQL enums
    JdbcTypeRegistry jdbcTypeRegistry =
        typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
    // Map NAMED_ENUM to VARCHAR
    jdbcTypeRegistry.addDescriptor(
        org.hibernate.type.SqlTypes.NAMED_ENUM,
        jdbcTypeRegistry.getDescriptor(java.sql.Types.VARCHAR));
  }
}
