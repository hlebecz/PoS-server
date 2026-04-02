package kurs.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.entity.Employee;

/**
 * Управление сотрудниками.
 *
 * <ul>
 *   <li>ADMIN — полный CRUD по всем точкам.
 *   <li>MANAGER — CRUD только по точкам, которыми он управляет (manager_id = его user_id).
 * </ul>
 */
public class EmployeeService {

  private final EmployeeDao employeeDao;
  private final StoreDao storeDao;

  public EmployeeService(EmployeeDao employeeDao, StoreDao storeDao) {
    this.employeeDao = employeeDao;
    this.storeDao = storeDao;
  }

  // -----------------------------------------------------------------------
  // Read
  // -----------------------------------------------------------------------

  public List<Employee> findAll(AuthenticatedUser caller) {
    requireAdminOrManager(caller);
    if (caller.isAdmin()) {
      return employeeDao.findAll();
    }
    // MANAGER: собираем сотрудников по всем своим точкам
    return storeDao.findByManagerId(caller.getUserId()).stream()
        .flatMap(s -> employeeDao.findByStoreId(s.getId()).stream())
        .distinct()
        .toList();
  }

  public List<Employee> findByStore(AuthenticatedUser caller, UUID storeId) {
    requireAdminOrManager(caller);
    if (caller.isManager()) {
      assertManagesStore(caller, storeId);
    }
    return employeeDao.findByStoreId(storeId);
  }

  public Employee findById(AuthenticatedUser caller, UUID id) {
    requireAdminOrManager(caller);
    Employee emp =
        employeeDao
            .findById(id)
            .orElseThrow(() -> new ServiceException("Сотрудник не найден", "EMPLOYEE_NOT_FOUND"));
    if (caller.isManager()) {
      assertManagesStore(caller, emp.getStore().getId());
    }
    return emp;
  }

  // -----------------------------------------------------------------------
  // Write
  // -----------------------------------------------------------------------

  public Employee create(AuthenticatedUser caller, Employee employee) {
    requireAdminOrManager(caller);
    if (caller.isManager()) {
      assertManagesStore(caller, employee.getStore().getId());
    }
    return employeeDao.save(employee);
  }

  public Employee update(AuthenticatedUser caller, Employee employee) {
    requireAdminOrManager(caller);
    Employee existing =
        employeeDao
            .findById(employee.getId())
            .orElseThrow(() -> new ServiceException("Сотрудник не найден", "EMPLOYEE_NOT_FOUND"));
    if (caller.isManager()) {
      assertManagesStore(caller, existing.getStore().getId());
    }
    return employeeDao.update(employee);
  }

  public void delete(AuthenticatedUser caller, UUID id) {
    requireAdminOrManager(caller);
    Employee emp =
        employeeDao
            .findById(id)
            .orElseThrow(() -> new ServiceException("Сотрудник не найден", "EMPLOYEE_NOT_FOUND"));
    if (caller.isManager()) {
      assertManagesStore(caller, emp.getStore().getId());
    }
    employeeDao.delete(emp);
  }

  /** Мягкое увольнение — проставляет firedAt. */
  public Employee fire(AuthenticatedUser caller, UUID id, LocalDate firedAt) {
    requireAdminOrManager(caller);
    Employee emp =
        employeeDao
            .findById(id)
            .orElseThrow(() -> new ServiceException("Сотрудник не найден", "EMPLOYEE_NOT_FOUND"));
    if (caller.isManager()) {
      assertManagesStore(caller, emp.getStore().getId());
    }
    emp.setFiredAt(firedAt == null ? LocalDate.now() : firedAt);
    return employeeDao.update(emp);
  }

  // -----------------------------------------------------------------------
  // Guards
  // -----------------------------------------------------------------------

  private void requireAdminOrManager(AuthenticatedUser caller) {
    if (!caller.isAdmin() && !caller.isManager()) {
      throw new AccessDeniedException("Требуется роль ADMIN или MANAGER");
    }
  }

  /** Проверяет, что данная точка входит в список точек, которыми управляет MANAGER. */
  private void assertManagesStore(AuthenticatedUser caller, UUID storeId) {
    boolean manages =
        storeDao.findByManagerId(caller.getUserId()).stream()
            .anyMatch(s -> s.getId().equals(storeId));
    if (!manages) {
      throw new AccessDeniedException("Нет доступа к этой торговой точке");
    }
  }
}
