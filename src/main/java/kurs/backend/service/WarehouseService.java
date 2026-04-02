package kurs.backend.service;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateWarehouseRequest;
import kurs.backend.domain.dto.request.UpdateWarehouseRequest;
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

  private final WarehouseDao warehouseDao;
  private final LocationDao locationDao;

  public List<WarehouseResponse> findAll(AuthenticatedUser caller) {
    requireReadAccess(caller);
    return warehouseDao.findAll().stream().map(WarehouseResponse::from).toList();
  }

  public List<WarehouseResponse> findAllActive(AuthenticatedUser caller) {
    requireReadAccess(caller);
    return warehouseDao.findAllActive().stream().map(WarehouseResponse::from).toList();
  }

  public WarehouseResponse findById(AuthenticatedUser caller, UUID id) {
    requireReadAccess(caller);
    return WarehouseResponse.from(getOrThrow(id));
  }

  public WarehouseResponse create(AuthenticatedUser caller, CreateWarehouseRequest req) {
    requireAdmin(caller);
    req.validate();

    Location location =
        req.getLocationId() != null
            ? locationDao
                .findById(req.getLocationId())
                .orElseThrow(() -> new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND"))
            : null;

    Warehouse wh = new Warehouse();
    wh.setName(req.getName());
    wh.setPhone(req.getPhone());
    wh.setIsActive(true);
    wh.setLocation(location);

    return WarehouseResponse.from(warehouseDao.save(wh));
  }

  public WarehouseResponse update(AuthenticatedUser caller, UpdateWarehouseRequest req) {
    requireAdmin(caller);
    req.validate();

    Warehouse wh = getOrThrow(req.getId());

    if (req.getName() != null && !req.getName().isBlank()) wh.setName(req.getName());
    if (req.getPhone() != null) wh.setPhone(req.getPhone());
    if (req.getLocationId() != null) {
      Location loc =
          locationDao
              .findById(req.getLocationId())
              .orElseThrow(() -> new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND"));
      wh.setLocation(loc);
    }
    if (req.getIsActive() != null) wh.setIsActive(req.getIsActive());

    return WarehouseResponse.from(warehouseDao.update(wh));
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    warehouseDao.delete(getOrThrow(id));
  }

  public WarehouseResponse deactivate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    Warehouse wh = getOrThrow(id);
    wh.setIsActive(false);
    return WarehouseResponse.from(warehouseDao.update(wh));
  }

  public WarehouseResponse activate(AuthenticatedUser caller, UUID id) {
    requireAdmin(caller);
    Warehouse wh = getOrThrow(id);
    wh.setIsActive(true);
    return WarehouseResponse.from(warehouseDao.update(wh));
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
