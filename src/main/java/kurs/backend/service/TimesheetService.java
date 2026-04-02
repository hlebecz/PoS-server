package kurs.backend.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.response.TimesheetResponse;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.dao.TimesheetDao;

/**
 * Просмотр табелей рабочего времени (только чтение). Запись создаётся и закрывается автоматически в
 * AuthService.
 *
 * <p>MANAGER — видит табели сотрудников своих точек. ACCOUNTANT, ADMIN — видят все табели.
 */
@AllArgsConstructor
public class TimesheetService {

  private final TimesheetDao timesheetDao;
  private final EmployeeDao employeeDao;
  private final StoreDao storeDao;

  public List<TimesheetResponse> findByEmployee(AuthenticatedUser caller, UUID employeeId) {
    requireTimesheetRead(caller);
    assertCanViewEmployee(caller, employeeId);
    return timesheetDao.findByEmployeeId(employeeId).stream().map(TimesheetResponse::from).toList();
  }

  public List<TimesheetResponse> findByEmployeeAndPeriod(
      AuthenticatedUser caller, UUID employeeId, LocalDate from, LocalDate to) {
    requireTimesheetRead(caller);
    assertCanViewEmployee(caller, employeeId);
    return timesheetDao.findByEmployeeIdAndPeriod(employeeId, from, to).stream()
        .map(TimesheetResponse::from)
        .toList();
  }

  public List<TimesheetResponse> findByStore(
      AuthenticatedUser caller, UUID storeId, LocalDate from, LocalDate to) {
    requireTimesheetRead(caller);
    if (caller.isManager()) assertManagesStore(caller, storeId);
    return employeeDao.findByStoreId(storeId).stream()
        .flatMap(e -> timesheetDao.findByEmployeeIdAndPeriod(e.getId(), from, to).stream())
        .map(TimesheetResponse::from)
        .toList();
  }

  public List<TimesheetResponse> findAll(AuthenticatedUser caller) {
    requireAccountantOrAdmin(caller);
    return timesheetDao.findAll().stream().map(TimesheetResponse::from).toList();
  }

  public List<TimesheetResponse> findAllByPeriod(
      AuthenticatedUser caller, LocalDate from, LocalDate to) {
    requireAccountantOrAdmin(caller);
    return employeeDao.findAll().stream()
        .flatMap(e -> timesheetDao.findByEmployeeIdAndPeriod(e.getId(), from, to).stream())
        .map(TimesheetResponse::from)
        .toList();
  }

  private void requireTimesheetRead(AuthenticatedUser caller) {
    if (caller.isGuest() || caller.isCashier())
      throw new AccessDeniedException("Нет доступа к табелям");
  }

  private void requireAccountantOrAdmin(AuthenticatedUser caller) {
    if (!caller.isAccountant() && !caller.isAdmin())
      throw new AccessDeniedException("Требуется роль ACCOUNTANT или ADMIN");
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
              if (!manages)
                throw new AccessDeniedException("Нет доступа к табелю данного сотрудника");
            });
  }

  private void assertManagesStore(AuthenticatedUser caller, UUID storeId) {
    boolean manages =
        storeDao.findByManagerId(caller.getUserId()).stream()
            .anyMatch(s -> s.getId().equals(storeId));
    if (!manages) throw new AccessDeniedException("Нет доступа к этой торговой точке");
  }
}
