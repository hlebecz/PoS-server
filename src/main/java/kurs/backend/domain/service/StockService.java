package kurs.backend.domain.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final Logger log = LogManager.getLogger(StockService.class);
  private static final Logger auditLog = LogManager.getLogger("kurs.backend.audit");

  private final StockDao stockDao;
  private final StoreDao storeDao;
  private final WarehouseDao warehouseDao;
  private final ProductDao productDao;

  public List<StockResponse> findAll(AuthenticatedUser caller) {
    log.debug("Finding all stock: userId={}", caller.getUserId());
    List<StockResponse> result = stockDao.findAll().stream().map(StockResponse::from).toList();
    log.debug("Found {} stock records", result.size());
    return result;
  }

  public List<StockResponse> findByStorageLocation(AuthenticatedUser caller, UUID locationId) {
    log.debug(
        "Finding stock by storage location: locationId={}, userId={}",
        locationId,
        caller.getUserId());
    List<StockResponse> result =
        stockDao.findByStorageLocationId(locationId).stream().map(StockResponse::from).toList();
    log.debug("Found {} stock records for locationId={}", result.size(), locationId);
    return result;
  }

  public List<StockResponse> findByProduct(AuthenticatedUser caller, UUID productId) {
    log.debug("Finding stock by product: productId={}, userId={}", productId, caller.getUserId());
    List<StockResponse> result =
        stockDao.findByProductId(productId).stream().map(StockResponse::from).toList();
    log.debug("Found {} stock records for productId={}", result.size(), productId);
    return result;
  }

  public StockResponse set(AuthenticatedUser caller, SetStockRequest req) {
    requireStockWrite(caller, req.getStorageLocationId());
    req.validate();

    log.info(
        "Setting stock: storageLocationId={}, productId={}, quantity={}, userId={}",
        req.getStorageLocationId(),
        req.getProductId(),
        req.getQuantity(),
        caller.getUserId());

    StockId id = new StockId(req.getStorageLocationId(), req.getProductId());
    Stock stock = stockDao.findById(id).orElse(null);

    if (stock != null) {
      int oldQuantity = stock.getQuantity();
      stock.setQuantity(req.getQuantity());
      Stock updated = stockDao.update(stock);
      log.info(
          "Stock updated: storageLocationId={}, productId={}, oldQuantity={}, newQuantity={}",
          req.getStorageLocationId(),
          req.getProductId(),
          oldQuantity,
          req.getQuantity());
      auditLog.info(
          "Stock updated: storageLocationId={}, productId={}, oldQuantity={}, newQuantity={}, userId={}",
          req.getStorageLocationId(),
          req.getProductId(),
          oldQuantity,
          req.getQuantity(),
          caller.getUserId());
      return StockResponse.from(updated);
    }

    // Создаём новую запись
    StorageLocation location = resolveLocation(req.getStorageLocationId());
    Product product =
        productDao
            .findById(req.getProductId())
            .orElseThrow(
                () -> {
                  log.warn("Product not found: productId={}", req.getProductId());
                  return new ServiceException("Товар не найден", "PRODUCT_NOT_FOUND");
                });

    stock =
        Stock.builder()
            .storageLocation(location)
            .product(product)
            .quantity(req.getQuantity())
            .build();

    Stock saved = stockDao.save(stock);
    log.info(
        "Stock created: storageLocationId={}, productId={}, quantity={}",
        req.getStorageLocationId(),
        req.getProductId(),
        req.getQuantity());
    auditLog.info(
        "Stock created: storageLocationId={}, productId={}, quantity={}, userId={}",
        req.getStorageLocationId(),
        req.getProductId(),
        req.getQuantity(),
        caller.getUserId());

    return StockResponse.from(saved);
  }

  public void delete(AuthenticatedUser caller, UUID storageLocationId, UUID productId) {
    requireStockWrite(caller, storageLocationId);
    log.info(
        "Deleting stock: storageLocationId={}, productId={}, userId={}",
        storageLocationId,
        productId,
        caller.getUserId());
    StockId id = new StockId(storageLocationId, productId);
    Stock stock =
        stockDao
            .findById(id)
            .orElseThrow(
                () -> {
                  log.warn(
                      "Stock not found for deletion: storageLocationId={}, productId={}",
                      storageLocationId,
                      productId);
                  return new ServiceException("Запись остатка не найдена", "STOCK_NOT_FOUND");
                });
    stockDao.delete(stock);
    log.info(
        "Stock deleted successfully: storageLocationId={}, productId={}",
        storageLocationId,
        productId);
    auditLog.info(
        "Stock deleted: storageLocationId={}, productId={}, quantity={}, userId={}",
        storageLocationId,
        productId,
        stock.getQuantity(),
        caller.getUserId());
  }

  public void deduct(UUID storageLocationId, UUID productId, int quantity) {
    log.info(
        "Deducting stock: storageLocationId={}, productId={}, quantity={}",
        storageLocationId,
        productId,
        quantity);
    StockId id = new StockId(storageLocationId, productId);
    Stock stock =
        stockDao
            .findById(id)
            .orElseThrow(
                () -> {
                  log.error(
                      "Stock not found for deduction: storageLocationId={}, productId={}",
                      storageLocationId,
                      productId);
                  return new ServiceException(
                      "Товар отсутствует на складе данной точки", "STOCK_NOT_FOUND");
                });
    if (stock.getQuantity() < quantity) {
      log.warn(
          "Insufficient stock: storageLocationId={}, productId={}, available={}, requested={}",
          storageLocationId,
          productId,
          stock.getQuantity(),
          quantity);
      throw new ServiceException(
          "Недостаточно товара: доступно " + stock.getQuantity() + ", запрошено " + quantity,
          "STOCK_INSUFFICIENT");
    }
    int oldQuantity = stock.getQuantity();
    stock.setQuantity(stock.getQuantity() - quantity);
    stockDao.update(stock);
    log.info(
        "Stock deducted successfully: storageLocationId={}, productId={}, oldQuantity={}, newQuantity={}",
        storageLocationId,
        productId,
        oldQuantity,
        stock.getQuantity());
    auditLog.info(
        "Stock deducted: storageLocationId={}, productId={}, quantity={}, oldQuantity={}, newQuantity={}",
        storageLocationId,
        productId,
        quantity,
        oldQuantity,
        stock.getQuantity());
  }

  public void restore(UUID storageLocationId, UUID productId, int quantity) {
    log.info(
        "Restoring stock: storageLocationId={}, productId={}, quantity={}",
        storageLocationId,
        productId,
        quantity);
    StockId id = new StockId(storageLocationId, productId);
    Stock stock =
        stockDao
            .findById(id)
            .orElseThrow(
                () -> {
                  log.error(
                      "Stock not found for restore: storageLocationId={}, productId={}",
                      storageLocationId,
                      productId);
                  return new ServiceException(
                      "Запись остатка не найдена для возврата", "STOCK_NOT_FOUND");
                });
    int oldQuantity = stock.getQuantity();
    stock.setQuantity(stock.getQuantity() + quantity);
    stockDao.update(stock);
    log.info(
        "Stock restored successfully: storageLocationId={}, productId={}, oldQuantity={}, newQuantity={}",
        storageLocationId,
        productId,
        oldQuantity,
        stock.getQuantity());
    auditLog.info(
        "Stock restored: storageLocationId={}, productId={}, quantity={}, oldQuantity={}, newQuantity={}",
        storageLocationId,
        productId,
        quantity,
        oldQuantity,
        stock.getQuantity());
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
