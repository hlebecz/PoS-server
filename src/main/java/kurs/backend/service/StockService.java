package kurs.backend.service;

import java.util.List;
import java.util.UUID;

import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.StockDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.entity.Stock;
import kurs.backend.domain.persistence.entity.StockId;

/**
 * Управление остатками товара.
 *
 * <ul>
 *   <li>GUEST — только просмотр (глобально и по точке).
 *   <li>MANAGER — CRUD, но только по точкам под своим управлением.
 *   <li>ACCOUNTANT, ADMIN — полный CRUD.
 * </ul>
 */
public class StockService {

  private final StockDao stockDao;
  private final StoreDao storeDao;

  public StockService(StockDao stockDao, StoreDao storeDao) {
    this.stockDao = stockDao;
    this.storeDao = storeDao;
  }

  public List<Stock> findAll(AuthenticatedUser caller) {
    return stockDao.findAll();
  }

  public List<Stock> findByStorageLocation(AuthenticatedUser caller, UUID storageLocationId) {
    return stockDao.findByStorageLocationId(storageLocationId);
  }

  public List<Stock> findByProduct(AuthenticatedUser caller, UUID productId) {
    return stockDao.findByProductId(productId);
  }

  public Stock create(AuthenticatedUser caller, Stock stock) {
    requireStockWrite(caller, stock.getStorageLocation().getId());
    return stockDao.save(stock);
  }

  public Stock update(AuthenticatedUser caller, Stock stock) {
    requireStockWrite(caller, stock.getStorageLocation().getId());
    return stockDao.update(stock);
  }

  public void delete(AuthenticatedUser caller, StockId id) {
    Stock stock =
        stockDao
            .findById(id)
            .orElseThrow(
                () -> new ServiceException("Запись остатка не найдена", "STOCK_NOT_FOUND"));
    requireStockWrite(caller, stock.getStorageLocation().getId());
    stockDao.delete(stock);
  }

  /**
   * Списывает quantity единиц товара productId со склада storageLocationId. Если остатка
   * недостаточно — бросает ServiceException. Вызывается внутри транзакции SaleService, поэтому
   * права не проверяем.
   */
  public void deduct(UUID storageLocationId, UUID productId, int quantity) {
    StockId id = new StockId(storageLocationId, productId);
    Stock stock =
        stockDao
            .findById(id)
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Товар отсутствует на складе данной точки", "STOCK_NOT_FOUND"));

    if (stock.getQuantity() < quantity) {
      throw new ServiceException(
          "Недостаточно товара: доступно " + stock.getQuantity() + ", запрошено " + quantity,
          "STOCK_INSUFFICIENT");
    }
    stock.setQuantity(stock.getQuantity() - quantity);
    stockDao.update(stock);
  }

  /** Возвращает quantity единиц товара (при отмене продажи / возврате). */
  public void restore(UUID storageLocationId, UUID productId, int quantity) {
    StockId id = new StockId(storageLocationId, productId);
    Stock stock =
        stockDao
            .findById(id)
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Запись остатка не найдена для возврата", "STOCK_NOT_FOUND"));
    stock.setQuantity(stock.getQuantity() + quantity);
    stockDao.update(stock);
  }

  private void requireStockWrite(AuthenticatedUser caller, UUID storageLocationId) {
    if (caller.isGuest()) {
      throw new AccessDeniedException("GUEST не может изменять остатки");
    }
    if (caller.isCashier()) {
      throw new AccessDeniedException("CASHIER не может изменять остатки напрямую");
    }
    if (caller.isManager()) {
      boolean manages =
          storeDao.findByManagerId(caller.getUserId()).stream()
              .anyMatch(s -> s.getId().equals(storageLocationId));
      if (!manages) {
        throw new AccessDeniedException("MANAGER: нет доступа к этому хранилищу");
      }
    }
  }
}
