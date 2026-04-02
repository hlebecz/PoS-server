package kurs.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.dao.TimesheetDao;
import kurs.backend.domain.persistence.entity.Timesheet;

/**
 * Просмотр табелей рабочего времени.
 *
 * <p>Запись Timesheet создаётся и закрывается автоматически в {@link AuthService}. Данный сервис
 * предоставляет только чтение:
 *
 * <ul>
 *   <li>MANAGER — видит табели сотрудников своих точек.
 *   <li>ACCOUNTANT, ADMIN — видят все табели.
 * </ul>
 */
public class TimesheetService {

  private final TimesheetDao timesheetDao;
  private final EmployeeDao employeeDao;
  private final StoreDao storeDao;

  public TimesheetService(TimesheetDao timesheetDao, EmployeeDao employeeDao, StoreDao storeDao) {
    this.timesheetDao = timesheetDao;
    this.employeeDao = employeeDao;
    this.storeDao = storeDao;
  }

  public List<Timesheet> findByEmployee(AuthenticatedUser caller, UUID employeeId) {
    requireTimesheetRead(caller);
    assertCanViewEmployee(caller, employeeId);
    return timesheetDao.findByEmployeeId(employeeId);
  }

  public List<Timesheet> findByEmployeeAndPeriod(
      AuthenticatedUser caller, UUID employeeId, LocalDate from, LocalDate to) {
    requireTimesheetRead(caller);
    assertCanViewEmployee(caller, employeeId);
    return timesheetDao.findByEmployeeIdAndPeriod(employeeId, from, to);
  }

  public List<Timesheet> findByStore(
      AuthenticatedUser caller, UUID storeId, LocalDate from, LocalDate to) {
    requireTimesheetRead(caller);
    if (caller.isManager()) {
      assertManagesStore(caller, storeId);
    }
    return employeeDao.findByStoreId(storeId).stream()
        .flatMap(e -> timesheetDao.findByEmployeeIdAndPeriod(e.getId(), from, to).stream())
        .toList();
  }

  public List<Timesheet> findAll(AuthenticatedUser caller) {
    requireAccountantOrAdmin(caller);
    return timesheetDao.findAll();
  }

  public List<Timesheet> findAllByPeriod(AuthenticatedUser caller, LocalDate from, LocalDate to) {
    requireAccountantOrAdmin(caller);
    return employeeDao.findAll().stream()
        .flatMap(e -> timesheetDao.findByEmployeeIdAndPeriod(e.getId(), from, to).stream())
        .toList();
  }

  private void requireTimesheetRead(AuthenticatedUser caller) {
    if (caller.isGuest() || caller.isCashier()) {
      throw new AccessDeniedException("Нет доступа к табелям");
    }
  }

  private void requireAccountantOrAdmin(AuthenticatedUser caller) {
    if (!caller.isAccountant() && !caller.isAdmin()) {
      throw new AccessDeniedException("Требуется роль ACCOUNTANT или ADMIN");
    }
  }

  private void assertCanViewEmployee(AuthenticatedUser caller, UUID employeeId) {
    if (caller.isAdmin() || caller.isAccountant()) return;
    employeeDao
        .findById(employeeId)
        .ifPresent(
            emp -> {
              boolean manages =
                  storeDao.findByManagerId(caller.getUserId()).stream()
                      .anyMatch(s -> s.getId().equals(emp.getStore().getId()));
              if (!manages) {
                throw new AccessDeniedException("Нет доступа к табелю данного сотрудника");
              }
            });
  }

  private void assertManagesStore(AuthenticatedUser caller, UUID storeId) {
    boolean manages =
        storeDao.findByManagerId(caller.getUserId()).stream()
            .anyMatch(s -> s.getId().equals(storeId));
    if (!manages) {
      throw new AccessDeniedException("Нет доступа к этой торговой точке");
    }
  }
}
