package kurs.backend.domain.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateStoreRequest;
import kurs.backend.domain.dto.request.UpdateStoreRequest;
import kurs.backend.domain.dto.response.StoreBasicResponse;
import kurs.backend.domain.dto.response.StoreResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.LocationDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.dao.UserDao;
import kurs.backend.domain.persistence.dao.WarehouseDao;
import kurs.backend.domain.persistence.entity.Location;
import kurs.backend.domain.persistence.entity.Store;
import kurs.backend.domain.persistence.entity.User;
import kurs.backend.domain.persistence.entity.Warehouse;

/**
 * Управление торговыми точками.
 *
 * <ul>
 *   <li>ADMIN — полный CRUD.
 *   <li>MANAGER — чтение своих точек, обновление (имя, телефон, локация) своих точек. Создание и
 *       удаление — только ADMIN.
 * </ul>
 */
@AllArgsConstructor
public class StoreService {

  private static final Logger log = LogManager.getLogger(StoreService.class);
  private static final Logger auditLog = LogManager.getLogger("kurs.backend.audit");

  private final StoreDao storeDao;
  private final UserDao userDao;
  private final WarehouseDao warehouseDao;
  private final LocationDao locationDao;

  public List<StoreResponse> findAll(AuthenticatedUser caller) {
    requireAdminOrManager(caller);
    log.info("Finding all stores: userId={}, role={}", caller.getUserId(), caller.getRole());

    List<StoreResponse> result;
    if (caller.isAdmin()) {
      result = storeDao.findAll().stream().map(StoreResponse::from).toList();
    } else {
      // MANAGER видит только свои точки
      result =
          storeDao.findByManagerId(caller.getUserId()).stream().map(StoreResponse::from).toList();
    }

    log.debug("Found {} stores", result.size());
    return result;
  }

  public List<StoreResponse> findAllActive(AuthenticatedUser caller) {
    requireAdminOrManager(caller);
    log.info("Finding all active stores: userId={}, role={}", caller.getUserId(), caller.getRole());

    List<StoreResponse> result;
    if (caller.isAdmin()) {
      result = storeDao.findAllActive().stream().map(StoreResponse::from).toList();
    } else {
      result =
          storeDao.findByManagerId(caller.getUserId()).stream()
              .filter(Store::getIsActive)
              .map(StoreResponse::from)
              .toList();
    }

    log.debug("Found {} active stores", result.size());
    return result;
  }

  /**
   * Возвращает базовую информацию об активных магазинах (только id и name). Доступно всем
   * аутентифицированным пользователям для использования в фильтрах и пикерах.
   */
  public List<StoreBasicResponse> findAllActiveBasic(AuthenticatedUser caller) {
    // Нет проверки прав - все аутентифицированные пользователи могут видеть список активных
    // магазинов
    log.debug("Finding all active stores (basic): userId={}", caller.getUserId());
    List<StoreBasicResponse> result =
        storeDao.findAllActive().stream().map(StoreBasicResponse::from).toList();
    log.debug("Found {} active stores (basic)", result.size());
    return result;
  }

  public StoreResponse findById(AuthenticatedUser caller, UUID id) {
    requireAdminOrManager(caller);
    log.debug("Finding store by id: storeId={}, userId={}", id, caller.getUserId());
    Store store = getOrThrow(id);
    if (caller.isManager()) assertManagesStore(caller, store.getId());
    return StoreResponse.from(store);
  }

  public List<StoreResponse> findByWarehouse(AuthenticatedUser caller, UUID warehouseId) {
    requireAdminOrManager(caller);
    log.info(
        "Finding stores by warehouse: warehouseId={}, userId={}", warehouseId, caller.getUserId());
    List<StoreResponse> result =
        storeDao.findByWarehouseId(warehouseId).stream().map(StoreResponse::from).toList();

    if (caller.isManager()) {
      List<UUID> myStoreIds =
          storeDao.findByManagerId(caller.getUserId()).stream().map(Store::getId).toList();
      result = result.stream().filter(s -> myStoreIds.contains(s.getId())).toList();
    }

    log.debug("Found {} stores for warehouseId={}", result.size(), warehouseId);
    return result;
  }

  public StoreResponse create(AuthenticatedUser caller, CreateStoreRequest req) {
    requireAdmin(caller);
    req.validate();

    log.info(
        "Creating store: name={}, managerId={}, warehouseId={}, userId={}",
        req.getName(),
        req.getManagerId(),
        req.getWarehouseId(),
        caller.getUserId());

    User manager =
        req.getManagerId() != null
            ? userDao
                .findById(req.getManagerId())
                .orElseThrow(
                    () -> {
                      log.warn("Manager not found: managerId={}", req.getManagerId());
                      return new ServiceException("Менеджер не найден", "USER_NOT_FOUND");
                    })
            : null;

    Warehouse warehouse =
        req.getWarehouseId() != null
            ? warehouseDao
                .findById(req.getWarehouseId())
                .orElseThrow(
                    () -> {
                      log.warn("Warehouse not found: warehouseId={}", req.getWarehouseId());
                      return new ServiceException("Склад не найден", "WAREHOUSE_NOT_FOUND");
                    })
            : null;

    Location location =
        req.getLocationId() != null
            ? locationDao
                .findById(req.getLocationId())
                .orElseThrow(
                    () -> {
                      log.warn("Location not found: locationId={}", req.getLocationId());
                      return new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND");
                    })
            : null;

    Store store = new Store();
    store.setName(req.getName());
    store.setPhone(req.getPhone());
    store.setIsActive(true);
    store.setManager(manager);
    store.setWarehouse(warehouse);
    store.setLocation(location);

    Store saved = storeDao.save(store);
    log.info("Store created successfully: storeId={}, name={}", saved.getId(), saved.getName());
    auditLog.info(
        "Store created: storeId={}, name={}, managerId={}, warehouseId={}, userId={}",
        saved.getId(),
        saved.getName(),
        req.getManagerId(),
        req.getWarehouseId(),
        caller.getUserId());

    return StoreResponse.from(saved);
  }

  /** ADMIN может менять всё. MANAGER может менять только name, phone, locationId своей точки. */
  public StoreResponse update(AuthenticatedUser caller, UpdateStoreRequest req) {
    requireAdminOrManager(caller);
    req.validate();

    log.info("Updating store: storeId={}, userId={}", req.getId(), caller.getUserId());

    Store store = getOrThrow(req.getId());
    if (caller.isManager()) assertManagesStore(caller, store.getId());

    if (req.getName() != null && !req.getName().isBlank()) {
      log.debug("Updating store name: storeId={}, newName={}", req.getId(), req.getName());
      store.setName(req.getName());
    }
    if (req.getPhone() != null && !req.getPhone().isBlank()) store.setPhone(req.getPhone());
    if (req.getLocationId() != null) {
      Location loc =
          locationDao
              .findById(req.getLocationId())
              .orElseThrow(
                  () -> {
                    log.warn("Location not found: locationId={}", req.getLocationId());
                    return new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND");
                  });
      store.setLocation(loc);
    }

    // Следующие поля — только для ADMIN
    if (caller.isAdmin()) {
      if (req.getManagerId() != null) {
        User mgr =
            userDao
                .findById(req.getManagerId())
                .orElseThrow(
                    () -> {
                      log.warn("Manager not found: managerId={}", req.getManagerId());
                      return new ServiceException("Менеджер не найден", "USER_NOT_FOUND");
                    });
        log.debug(
            "Updating store manager: storeId={}, newManagerId={}", req.getId(), req.getManagerId());
        store.setManager(mgr);
      }
      if (req.getWarehouseId() != null) {
        Warehouse wh =
            warehouseDao
                .findById(req.getWarehouseId())
                .orElseThrow(
                    () -> {
                      log.warn("Warehouse not found: warehouseId={}", req.getWarehouseId());
                      return new ServiceException("Склад не найден", "WAREHOUSE_NOT_FOUND");
                    });
        log.debug(
            "Updating store warehouse: storeId={}, newWarehouseId={}",
            req.getId(),
            req.getWarehouseId());
        store.setWarehouse(wh);
      }
      if (req.getIsActive() != null) store.setIsActive(req.getIsActive());
    }

    Store updated = storeDao.update(store);
    log.info("Store updated successfully: storeId={}", updated.getId());
    return StoreResponse.from(updated);
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Deleting store: storeId={}, userId={}", id, caller.getUserId());
    Store store = getOrThrow(id);
    storeDao.delete(store);
    log.info("Store deleted successfully: storeId={}", id);
    auditLog.info(
        "Store deleted: storeId={}, name={}, userId={}", id, store.getName(), caller.getUserId());
  }

  public StoreResponse deactivate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Deactivating store: storeId={}, userId={}", id, caller.getUserId());
    Store store = getOrThrow(id);
    store.setIsActive(false);
    Store updated = storeDao.update(store);
    log.info("Store deactivated successfully: storeId={}", id);
    auditLog.info(
        "Store deactivated: storeId={}, name={}, userId={}",
        id,
        store.getName(),
        caller.getUserId());
    return StoreResponse.from(updated);
  }

  public StoreResponse activate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Activating store: storeId={}, userId={}", id, caller.getUserId());
    Store store = getOrThrow(id);
    store.setIsActive(true);
    Store updated = storeDao.update(store);
    log.info("Store activated successfully: storeId={}", id);
    auditLog.info(
        "Store activated: storeId={}, name={}, userId={}", id, store.getName(), caller.getUserId());
    return StoreResponse.from(updated);
  }

  private Store getOrThrow(UUID id) {
    return storeDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Магазин не найден", "STORE_NOT_FOUND"));
  }

  private void requireAdmin(AuthenticatedUser caller) {
    if (!caller.isAdmin()) throw new AccessDeniedException("Требуется роль ADMIN");
  }

  private void requireAdminOrManager(AuthenticatedUser caller) {
    if (!caller.isAdmin() && !caller.isManager())
      throw new AccessDeniedException("Требуется роль ADMIN или MANAGER");
  }

  private void assertManagesStore(AuthenticatedUser caller, UUID storeId) {
    boolean manages =
        storeDao.findByManagerId(caller.getUserId()).stream()
            .anyMatch(s -> s.getId().equals(storeId));
    if (!manages) throw new AccessDeniedException("Нет доступа к этой торговой точке");
  }
}
