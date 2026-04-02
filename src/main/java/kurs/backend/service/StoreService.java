package kurs.backend.service;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateStoreRequest;
import kurs.backend.domain.dto.request.UpdateStoreRequest;
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

  private final StoreDao storeDao;
  private final UserDao userDao;
  private final WarehouseDao warehouseDao;
  private final LocationDao locationDao;

  public List<StoreResponse> findAll(AuthenticatedUser caller) {
    requireAdminOrManager(caller);
    if (caller.isAdmin()) return storeDao.findAll().stream().map(StoreResponse::from).toList();

    // MANAGER видит только свои точки
    return storeDao.findByManagerId(caller.getUserId()).stream().map(StoreResponse::from).toList();
  }

  public List<StoreResponse> findAllActive(AuthenticatedUser caller) {
    requireAdminOrManager(caller);
    if (caller.isAdmin())
      return storeDao.findAllActive().stream().map(StoreResponse::from).toList();

    return storeDao.findByManagerId(caller.getUserId()).stream()
        .filter(Store::getIsActive)
        .map(StoreResponse::from)
        .toList();
  }

  public StoreResponse findById(AuthenticatedUser caller, UUID id) {
    requireAdminOrManager(caller);
    Store store = getOrThrow(id);
    if (caller.isManager()) assertManagesStore(caller, store.getId());
    return StoreResponse.from(store);
  }

  public List<StoreResponse> findByWarehouse(AuthenticatedUser caller, UUID warehouseId) {
    requireAdminOrManager(caller);
    List<StoreResponse> result =
        storeDao.findByWarehouseId(warehouseId).stream().map(StoreResponse::from).toList();

    if (caller.isManager()) {
      List<UUID> myStoreIds =
          storeDao.findByManagerId(caller.getUserId()).stream().map(Store::getId).toList();
      return result.stream().filter(s -> myStoreIds.contains(s.getId())).toList();
    }
    return result;
  }

  public StoreResponse create(AuthenticatedUser caller, CreateStoreRequest req) {
    requireAdmin(caller);
    req.validate();

    User manager =
        req.getManagerId() != null
            ? userDao
                .findById(req.getManagerId())
                .orElseThrow(() -> new ServiceException("Менеджер не найден", "USER_NOT_FOUND"))
            : null;

    Warehouse warehouse =
        req.getWarehouseId() != null
            ? warehouseDao
                .findById(req.getWarehouseId())
                .orElseThrow(() -> new ServiceException("Склад не найден", "WAREHOUSE_NOT_FOUND"))
            : null;

    Location location =
        req.getLocationId() != null
            ? locationDao
                .findById(req.getLocationId())
                .orElseThrow(() -> new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND"))
            : null;

    Store store = new Store();
    store.setName(req.getName());
    store.setPhone(req.getPhone());
    store.setIsActive(true);
    store.setManager(manager);
    store.setWarehouse(warehouse);
    store.setLocation(location);

    return StoreResponse.from(storeDao.save(store));
  }

  /** ADMIN может менять всё. MANAGER может менять только name, phone, locationId своей точки. */
  public StoreResponse update(AuthenticatedUser caller, UpdateStoreRequest req) {
    requireAdminOrManager(caller);
    req.validate();

    Store store = getOrThrow(req.getId());
    if (caller.isManager()) assertManagesStore(caller, store.getId());

    if (req.getName() != null && !req.getName().isBlank()) store.setName(req.getName());
    if (req.getPhone() != null && !req.getPhone().isBlank()) store.setPhone(req.getPhone());
    if (req.getLocationId() != null) {
      Location loc =
          locationDao
              .findById(req.getLocationId())
              .orElseThrow(() -> new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND"));
      store.setLocation(loc);
    }

    // Следующие поля — только для ADMIN
    if (caller.isAdmin()) {
      if (req.getManagerId() != null) {
        User mgr =
            userDao
                .findById(req.getManagerId())
                .orElseThrow(() -> new ServiceException("Менеджер не найден", "USER_NOT_FOUND"));
        store.setManager(mgr);
      }
      if (req.getWarehouseId() != null) {
        Warehouse wh =
            warehouseDao
                .findById(req.getWarehouseId())
                .orElseThrow(() -> new ServiceException("Склад не найден", "WAREHOUSE_NOT_FOUND"));
        store.setWarehouse(wh);
      }
      if (req.getIsActive() != null) store.setIsActive(req.getIsActive());
    }

    return StoreResponse.from(storeDao.update(store));
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    storeDao.delete(getOrThrow(id));
  }

  public StoreResponse deactivate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    Store store = getOrThrow(id);
    store.setIsActive(false);
    return StoreResponse.from(storeDao.update(store));
  }

  public StoreResponse activate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    Store store = getOrThrow(id);
    store.setIsActive(true);
    return StoreResponse.from(storeDao.update(store));
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
