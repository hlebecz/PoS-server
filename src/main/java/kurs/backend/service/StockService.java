package kurs.backend.service;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.SetStockRequest;
import kurs.backend.domain.dto.response.StockResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.ProductDao;
import kurs.backend.domain.persistence.dao.StockDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.dao.WarehouseDao;
import kurs.backend.domain.persistence.entity.Product;
import kurs.backend.domain.persistence.entity.Stock;
import kurs.backend.domain.persistence.entity.StockId;
import kurs.backend.domain.persistence.entity.StorageLocation;

/**
 * Управление остатками. Чтение — все роли включая GUEST. Запись — ADMIN, ACCOUNTANT, MANAGER
 * (только свои точки).
 */
@AllArgsConstructor
public class StockService {

  private final StockDao stockDao;
  private final StoreDao storeDao;
  private final WarehouseDao warehouseDao;
  private final ProductDao productDao;

  public List<StockResponse> findAll(AuthenticatedUser caller) {
    return stockDao.findAll().stream().map(StockResponse::from).toList();
  }

  public List<StockResponse> findByStorageLocation(AuthenticatedUser caller, UUID locationId) {
    return stockDao.findByStorageLocationId(locationId).stream().map(StockResponse::from).toList();
  }

  public List<StockResponse> findByProduct(AuthenticatedUser caller, UUID productId) {
    return stockDao.findByProductId(productId).stream().map(StockResponse::from).toList();
  }

  public StockResponse set(AuthenticatedUser caller, SetStockRequest req) {
    requireStockWrite(caller, req.getStorageLocationId());
    req.validate();

    StockId id = new StockId(req.getStorageLocationId(), req.getProductId());
    Stock stock = stockDao.findById(id).orElse(null);

    if (stock != null) {
      stock.setQuantity(req.getQuantity());
      return StockResponse.from(stockDao.update(stock));
    }

    // Создаём новую запись
    StorageLocation location = resolveLocation(req.getStorageLocationId());
    Product product =
        productDao
            .findById(req.getProductId())
            .orElseThrow(() -> new ServiceException("Товар не найден", "PRODUCT_NOT_FOUND"));

    stock =
        Stock.builder()
            .storageLocation(location)
            .product(product)
            .quantity(req.getQuantity())
            .build();

    return StockResponse.from(stockDao.save(stock));
  }

  public void delete(AuthenticatedUser caller, UUID storageLocationId, UUID productId) {
    requireStockWrite(caller, storageLocationId);
    StockId id = new StockId(storageLocationId, productId);
    Stock stock =
        stockDao
            .findById(id)
            .orElseThrow(
                () -> new ServiceException("Запись остатка не найдена", "STOCK_NOT_FOUND"));
    stockDao.delete(stock);
  }

  public void deduct(UUID storageLocationId, UUID productId, int quantity) {
    StockId id = new StockId(storageLocationId, productId);
    Stock stock =
        stockDao
            .findById(id)
            .orElseThrow(
                () ->
                    new ServiceException(
                        "Товар отсутствует на складе данной точки", "STOCK_NOT_FOUND"));
    if (stock.getQuantity() < quantity)
      throw new ServiceException(
          "Недостаточно товара: доступно " + stock.getQuantity() + ", запрошено " + quantity,
          "STOCK_INSUFFICIENT");
    stock.setQuantity(stock.getQuantity() - quantity);
    stockDao.update(stock);
  }

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
    if (caller.isGuest()) throw new AccessDeniedException("GUEST не может изменять остатки");
    if (caller.isCashier())
      throw new AccessDeniedException("CASHIER не может изменять остатки напрямую");
    if (caller.isManager()) {
      boolean manages =
          storeDao.findByManagerId(caller.getUserId()).stream()
              .anyMatch(s -> s.getId().equals(storageLocationId));
      if (!manages) throw new AccessDeniedException("MANAGER: нет доступа к этому хранилищу");
    }
  }

  private StorageLocation resolveLocation(UUID id) {
    return storeDao
        .findById(id)
        .<StorageLocation>map(s -> s)
        .or(() -> warehouseDao.findById(id).map(w -> w))
        .orElseThrow(() -> new ServiceException("Хранилище не найдено", "STORAGE_NOT_FOUND"));
  }
}
