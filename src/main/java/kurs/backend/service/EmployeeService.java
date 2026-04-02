package kurs.backend.service;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateEmployeeRequest;
import kurs.backend.domain.dto.request.UpdateEmployeeRequest;
import kurs.backend.domain.dto.response.EmployeeResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.LocationDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.dao.UserDao;
import kurs.backend.domain.persistence.entity.Employee;
import kurs.backend.domain.persistence.entity.Location;
import kurs.backend.domain.persistence.entity.Store;
import kurs.backend.domain.persistence.entity.User;

/**
 * Управление сотрудниками. ADMIN — полный CRUD по всем точкам. MANAGER — CRUD только по точкам,
 * которыми он управляет.
 */
@AllArgsConstructor
public class EmployeeService {

  private final EmployeeDao employeeDao;
  private final StoreDao storeDao;
  private final UserDao userDao;
  private final LocationDao locationDao;

  public List<EmployeeResponse> findAll(AuthenticatedUser caller) {
    requireAdminOrManager(caller);
    if (caller.isAdmin())
      return employeeDao.findAll().stream().map(EmployeeResponse::from).toList();

    return storeDao.findByManagerId(caller.getUserId()).stream()
        .flatMap(s -> employeeDao.findByStoreId(s.getId()).stream())
        .distinct()
        .map(EmployeeResponse::from)
        .toList();
  }

  public List<EmployeeResponse> findByStore(AuthenticatedUser caller, UUID storeId) {
    requireAdminOrManager(caller);
    if (caller.isManager()) assertManagesStore(caller, storeId);
    return employeeDao.findByStoreId(storeId).stream().map(EmployeeResponse::from).toList();
  }

  public EmployeeResponse findById(AuthenticatedUser caller, UUID id) {
    requireAdminOrManager(caller);
    Employee emp = getOrThrow(id);
    if (caller.isManager()) assertManagesStore(caller, emp.getStore().getId());
    return EmployeeResponse.from(emp);
  }

  public EmployeeResponse create(AuthenticatedUser caller, CreateEmployeeRequest req) {
    requireAdminOrManager(caller);
    req.validate();

    Store store =
        storeDao
            .findById(req.getStoreId())
            .orElseThrow(() -> new ServiceException("Магазин не найден", "STORE_NOT_FOUND"));
    if (caller.isManager()) assertManagesStore(caller, store.getId());

    User user =
        req.getUserId() != null
            ? userDao
                .findById(req.getUserId())
                .orElseThrow(() -> new ServiceException("Пользователь не найден", "USER_NOT_FOUND"))
            : null;

    Location location =
        req.getLocationId() != null
            ? locationDao
                .findById(req.getLocationId())
                .orElseThrow(() -> new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND"))
            : null;

    Employee emp =
        Employee.builder()
            .store(store)
            .user(user)
            .location(location)
            .fullName(req.getFullName())
            .position(req.getPosition())
            .hourlyRate(req.getHourlyRate())
            .phone(req.getPhone())
            .email(req.getEmail())
            .hiredAt(req.getHiredAt())
            .build();

    return EmployeeResponse.from(employeeDao.save(emp));
  }

  public EmployeeResponse update(AuthenticatedUser caller, UpdateEmployeeRequest req) {
    requireAdminOrManager(caller);
    req.validate();

    Employee emp = getOrThrow(req.getId());
    if (caller.isManager()) assertManagesStore(caller, emp.getStore().getId());

    if (req.getStoreId() != null) {
      Store newStore =
          storeDao
              .findById(req.getStoreId())
              .orElseThrow(() -> new ServiceException("Магазин не найден", "STORE_NOT_FOUND"));
      if (caller.isManager()) assertManagesStore(caller, newStore.getId());
      emp.setStore(newStore);
    }
    if (req.getLocationId() != null) {
      Location loc =
          locationDao
              .findById(req.getLocationId())
              .orElseThrow(() -> new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND"));
      emp.setLocation(loc);
    }
    if (req.getFullName() != null && !req.getFullName().isBlank())
      emp.setFullName(req.getFullName());
    if (req.getPosition() != null && !req.getPosition().isBlank())
      emp.setPosition(req.getPosition());
    if (req.getHourlyRate() != null) emp.setHourlyRate(req.getHourlyRate());
    if (req.getPhone() != null) emp.setPhone(req.getPhone());
    if (req.getEmail() != null) emp.setEmail(req.getEmail());
    if (req.getFiredAt() != null) emp.setFiredAt(req.getFiredAt());

    return EmployeeResponse.from(employeeDao.update(emp));
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdminOrManager(caller);
    Employee emp = getOrThrow(id);
    if (caller.isManager()) assertManagesStore(caller, emp.getStore().getId());
    employeeDao.delete(emp);
  }

  private Employee getOrThrow(UUID id) {
    return employeeDao
        .findById(id)
        .orElseThrow(() -> new ServiceException("Сотрудник не найден", "EMPLOYEE_NOT_FOUND"));
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
