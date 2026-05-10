package kurs.backend.domain.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateWarehouseRequest;
import kurs.backend.domain.dto.request.UpdateWarehouseRequest;
import kurs.backend.domain.dto.response.WarehouseBasicResponse;
import kurs.backend.domain.dto.response.WarehouseResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.LocationDao;
import kurs.backend.domain.persistence.dao.WarehouseDao;
import kurs.backend.domain.persistence.entity.Location;
import kurs.backend.domain.persistence.entity.Warehouse;

/**
 * Управление складами.
 *
 * <ul>
 *   <li>ADMIN — полный CRUD.
 *   <li>MANAGER, ACCOUNTANT — только чтение.
 *   <li>GUEST, CASHIER — нет доступа.
 * </ul>
 */
@AllArgsConstructor
public class WarehouseService {

  private static final Logger log = LogManager.getLogger(WarehouseService.class);
  private static final Logger auditLog = LogManager.getLogger("kurs.backend.audit");

  private final WarehouseDao warehouseDao;
  private final LocationDao locationDao;

  public List<WarehouseResponse> findAll(AuthenticatedUser caller) {
    requireReadAccess(caller);
    log.info("Finding all warehouses: userId={}", caller.getUserId());
    List<WarehouseResponse> result =
        warehouseDao.findAll().stream().map(WarehouseResponse::from).toList();
    log.debug("Found {} warehouses", result.size());
    return result;
  }

  public List<WarehouseResponse> findAllActive(AuthenticatedUser caller) {
    requireReadAccess(caller);
    log.info("Finding all active warehouses: userId={}", caller.getUserId());
    List<WarehouseResponse> result =
        warehouseDao.findAllActive().stream().map(WarehouseResponse::from).toList();
    log.debug("Found {} active warehouses", result.size());
    return result;
  }

  /**
   * Возвращает базовую информацию об активных складах (только id и name). Доступно всем
   * аутентифицированным пользователям для использования в фильтрах и пикерах.
   */
  public List<WarehouseBasicResponse> findAllActiveBasic(AuthenticatedUser caller) {
    // Нет проверки прав - все аутентифицированные пользователи могут видеть список активных складов
    log.debug("Finding all active warehouses (basic): userId={}", caller.getUserId());
    List<WarehouseBasicResponse> result =
        warehouseDao.findAllActive().stream().map(WarehouseBasicResponse::from).toList();
    log.debug("Found {} active warehouses (basic)", result.size());
    return result;
  }

  public WarehouseResponse findById(AuthenticatedUser caller, UUID id) {
    requireReadAccess(caller);
    log.debug("Finding warehouse by id: warehouseId={}, userId={}", id, caller.getUserId());
    return WarehouseResponse.from(getOrThrow(id));
  }

  public WarehouseResponse create(AuthenticatedUser caller, CreateWarehouseRequest req) {
    requireAdmin(caller);
    req.validate();

    log.info(
        "Creating warehouse: name={}, locationId={}, userId={}",
        req.getName(),
        req.getLocationId(),
        caller.getUserId());

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

    Warehouse wh = new Warehouse();
    wh.setName(req.getName());
    wh.setPhone(req.getPhone());
    wh.setIsActive(true);
    wh.setLocation(location);

    Warehouse saved = warehouseDao.save(wh);
    log.info(
        "Warehouse created successfully: warehouseId={}, name={}", saved.getId(), saved.getName());
    auditLog.info(
        "Warehouse created: warehouseId={}, name={}, locationId={}, userId={}",
        saved.getId(),
        saved.getName(),
        req.getLocationId(),
        caller.getUserId());

    return WarehouseResponse.from(saved);
  }

  public WarehouseResponse update(AuthenticatedUser caller, UpdateWarehouseRequest req) {
    requireAdmin(caller);
    req.validate();

    log.info("Updating warehouse: warehouseId={}, userId={}", req.getId(), caller.getUserId());

    Warehouse wh = getOrThrow(req.getId());

    if (req.getName() != null && !req.getName().isBlank()) {
      log.debug("Updating warehouse name: warehouseId={}, newName={}", req.getId(), req.getName());
      wh.setName(req.getName());
    }
    if (req.getPhone() != null) wh.setPhone(req.getPhone());
    if (req.getLocationId() != null) {
      Location loc =
          locationDao
              .findById(req.getLocationId())
              .orElseThrow(
                  () -> {
                    log.warn("Location not found: locationId={}", req.getLocationId());
                    return new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND");
                  });
      wh.setLocation(loc);
    }
    if (req.getIsActive() != null) wh.setIsActive(req.getIsActive());

    Warehouse updated = warehouseDao.update(wh);
    log.info("Warehouse updated successfully: warehouseId={}", updated.getId());
    return WarehouseResponse.from(updated);
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Deleting warehouse: warehouseId={}, userId={}", id, caller.getUserId());
    Warehouse wh = getOrThrow(id);
    warehouseDao.delete(wh);
    log.info("Warehouse deleted successfully: warehouseId={}", id);
    auditLog.info(
        "Warehouse deleted: warehouseId={}, name={}, userId={}",
        id,
        wh.getName(),
        caller.getUserId());
  }

  public WarehouseResponse deactivate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Deactivating warehouse: warehouseId={}, userId={}", id, caller.getUserId());
    Warehouse wh = getOrThrow(id);
    wh.setIsActive(false);
    Warehouse updated = warehouseDao.update(wh);
    log.info("Warehouse deactivated successfully: warehouseId={}", id);
    auditLog.info(
        "Warehouse deactivated: warehouseId={}, name={}, userId={}",
        id,
        wh.getName(),
        caller.getUserId());
    return WarehouseResponse.from(updated);
  }

  public WarehouseResponse activate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    log.info("Activating warehouse: warehouseId={}, userId={}", id, caller.getUserId());
    Warehouse wh = getOrThrow(id);
    wh.setIsActive(true);
    Warehouse updated = warehouseDao.update(wh);
    log.info("Warehouse activated successfully: warehouseId={}", id);
    auditLog.info(
        "Warehouse activated: warehouseId={}, name={}, userId={}",
        id,
        wh.getName(),
        caller.getUserId());
    return WarehouseResponse.from(updated);
  }

  private Warehouse getOrThrow(UUID id) {
    return warehouseDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Склад не найден", "WAREHOUSE_NOT_FOUND"));
  }

  private void requireAdmin(AuthenticatedUser caller) {
    if (!caller.isAdmin()) throw new AccessDeniedException("Требуется роль ADMIN");
  }

  private void requireReadAccess(AuthenticatedUser caller) {
    if (caller.isGuest() || caller.isCashier())
      throw new AccessDeniedException("Нет доступа к складам");
  }
}
