package kurs.backend.domain.persistence.dao;

import java.util.UUID;

import kurs.backend.domain.persistence.entity.Location;

public class LocationDao extends GenericDaoImpl<Location, UUID> {

  public LocationDao() {
    super(Location.class);
  }
}
