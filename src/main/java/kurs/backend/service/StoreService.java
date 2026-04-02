package kurs.backend.service;

import lombok.Getter;
import lombok.Setter;

import kurs.backend.domain.persistence.dao.LocationDao;
import kurs.backend.domain.persistence.dao.StockDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.entity.Store;

@Getter
@Setter
public class StoreService {

  public final StoreDao storeDao;
  public final StockDao stockDao;
  public final LocationDao locationDao;

  public StoreService(StoreDao storeDao, StockDao stockDao, LocationDao locationDao) {
    this.storeDao = storeDao;
    this.stockDao = stockDao;
    this.locationDao = locationDao;
  }

  public Store create(Store store) {
    storeDao.save(store);
    return store;
  }
}
