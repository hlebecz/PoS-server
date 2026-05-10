package kurs.backend.domain.service;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.request.CreateEmployeeRequest;
import kurs.backend.domain.dto.request.FireEmployeeRequest;
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

  private static final Logger log = LogManager.getLogger(EmployeeService.class);
  private static final Logger auditLog = LogManager.getLogger("kurs.backend.audit");

  private final EmployeeDao employeeDao;
  private final StoreDao storeDao;
  private final UserDao userDao;
  private final LocationDao locationDao;

  public List<EmployeeResponse> findAll(AuthenticatedUser caller) {
    requireAdminOrManager(caller);
    log.info("Finding all employees: userId={}, role={}", caller.getUserId(), caller.getRole());

    List<EmployeeResponse> result;
    if (caller.isAdmin()) {
      result = employeeDao.findAll().stream().map(EmployeeResponse::from).toList();
    } else {
      result =
          storeDao.findByManagerId(caller.getUserId()).stream()
              .flatMap(s -> employeeDao.findByStoreId(s.getId()).stream())
              .distinct()
              .map(EmployeeResponse::from)
              .toList();
    }

    log.debug("Found {} employees", result.size());
    return result;
  }

  public List<EmployeeResponse> findByStore(AuthenticatedUser caller, UUID storeId) {
    requireAdminOrManager(caller);
    log.info("Finding employees by store: storeId={}, userId={}", storeId, caller.getUserId());
    if (caller.isManager()) assertManagesStore(caller, storeId);

    List<EmployeeResponse> result =
        employeeDao.findByStoreId(storeId).stream().map(EmployeeResponse::from).toList();
    log.debug("Found {} employees for storeId={}", result.size(), storeId);
    return result;
  }

  public EmployeeResponse findById(AuthenticatedUser caller, UUID id) {
    requireAdminOrManager(caller);
    log.debug("Finding employee by id: employeeId={}, userId={}", id, caller.getUserId());
    Employee emp = getOrThrow(id);
    if (caller.isManager()) assertManagesStore(caller, emp.getStore().getId());
    return EmployeeResponse.from(emp);
  }

  public EmployeeResponse create(AuthenticatedUser caller, CreateEmployeeRequest req) {
    requireAdminOrManager(caller);
    req.validate();

    log.info(
        "Creating employee: fullName={}, position={}, storeId={}, userId={}",
        req.getFullName(),
        req.getPosition(),
        req.getStoreId(),
        caller.getUserId());

    Store store =
        storeDao
            .findById(req.getStoreId())
            .orElseThrow(
                () -> {
                  log.warn("Store not found: storeId={}", req.getStoreId());
                  return new ServiceException("Магазин не найден", "STORE_NOT_FOUND");
                });
    if (caller.isManager()) assertManagesStore(caller, store.getId());

    User user =
        req.getUserId() != null
            ? userDao
                .findById(req.getUserId())
                .orElseThrow(
                    () -> {
                      log.warn("User not found: userId={}", req.getUserId());
                      return new ServiceException("Пользователь не найден", "USER_NOT_FOUND");
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

    Employee saved = employeeDao.save(emp);
    log.info(
        "Employee created successfully: employeeId={}, fullName={}, storeId={}",
        saved.getId(),
        saved.getFullName(),
        store.getId());
    auditLog.info(
        "Employee created: employeeId={}, fullName={}, position={}, storeId={}, userId={}",
        saved.getId(),
        saved.getFullName(),
        saved.getPosition(),
        store.getId(),
        caller.getUserId());

    return EmployeeResponse.from(saved);
  }

  public EmployeeResponse update(AuthenticatedUser caller, UpdateEmployeeRequest req) {
    requireAdminOrManager(caller);
    req.validate();

    log.info("Updating employee: employeeId={}, userId={}", req.getId(), caller.getUserId());

    Employee emp = getOrThrow(req.getId());
    if (caller.isManager()) assertManagesStore(caller, emp.getStore().getId());

    if (req.getStoreId() != null) {
      Store newStore =
          storeDao
              .findById(req.getStoreId())
              .orElseThrow(
                  () -> {
                    log.warn("Store not found: storeId={}", req.getStoreId());
                    return new ServiceException("Магазин не найден", "STORE_NOT_FOUND");
                  });
      if (caller.isManager()) assertManagesStore(caller, newStore.getId());
      log.debug(
          "Updating employee store: employeeId={}, newStoreId={}", req.getId(), newStore.getId());
      emp.setStore(newStore);
    }
    if (req.getLocationId() != null) {
      Location loc =
          locationDao
              .findById(req.getLocationId())
              .orElseThrow(
                  () -> {
                    log.warn("Location not found: locationId={}", req.getLocationId());
                    return new ServiceException("Локация не найдена", "LOCATION_NOT_FOUND");
                  });
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

    Employee updated = employeeDao.update(emp);
    log.info("Employee updated successfully: employeeId={}", updated.getId());
    return EmployeeResponse.from(updated);
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdminOrManager(caller);
    log.info("Deleting employee: employeeId={}, userId={}", id, caller.getUserId());
    Employee emp = getOrThrow(id);
    if (caller.isManager()) assertManagesStore(caller, emp.getStore().getId());
    employeeDao.delete(emp);
    log.info("Employee deleted successfully: employeeId={}", id);
    auditLog.info(
        "Employee deleted: employeeId={}, fullName={}, userId={}",
        id,
        emp.getFullName(),
        caller.getUserId());
  }

  public Employee fire(AuthenticatedUser caller, FireEmployeeRequest req) {
    requireAdminOrManager(caller);
    log.info(
        "Firing employee: employeeId={}, firedAt={}, userId={}",
        req.getId(),
        req.getFiredAt(),
        caller.getUserId());
    Employee emp = getOrThrow(req.getId());
    if (caller.isManager()) assertManagesStore(caller, emp.getStore().getId());
    emp.setFiredAt(req.getFiredAt());
    Employee updated = employeeDao.update(emp);
    log.info(
        "Employee fired successfully: employeeId={}, firedAt={}", req.getId(), req.getFiredAt());
    auditLog.info(
        "Employee fired: employeeId={}, fullName={}, firedAt={}, userId={}",
        req.getId(),
        emp.getFullName(),
        req.getFiredAt(),
        caller.getUserId());
    return updated;
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
