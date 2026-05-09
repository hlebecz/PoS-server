package kurs.backend.domain.persistence.dao;

import java.util.UUID;

import org.hibernate.SessionFactory;

import kurs.backend.domain.persistence.entity.Location;

public class LocationDao extends GenericDaoImpl<Location, UUID> {

  public LocationDao(SessionFactory sessionFactory) {
    super(Location.class, sessionFactory);
  }
}
