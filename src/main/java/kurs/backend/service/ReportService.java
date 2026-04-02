package kurs.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;

import kurs.backend.domain.dto.report.EmployeeStats;
import kurs.backend.domain.dto.report.SalesReportEntry;
import kurs.backend.domain.dto.report.StoreEfficiencyReport;
import kurs.backend.domain.dto.request.ReportRequest;
import kurs.backend.domain.excepton.AccessDeniedException;
import kurs.backend.domain.excepton.ServiceException;
import kurs.backend.domain.model.AuthenticatedUser;
import kurs.backend.domain.persistence.dao.EmployeeDao;
import kurs.backend.domain.persistence.dao.SaleDao;
import kurs.backend.domain.persistence.dao.StoreDao;
import kurs.backend.domain.persistence.dao.TimesheetDao;
import kurs.backend.domain.persistence.entity.Employee;
import kurs.backend.domain.persistence.entity.Sale;
import kurs.backend.domain.persistence.entity.Store;
import kurs.backend.domain.persistence.entity.Timesheet;

/** Генерация аналитических отчётов. Доступ: ACCOUNTANT и ADMIN. */
@AllArgsConstructor
public class ReportService {

  private final StoreDao storeDao;
  private final SaleDao saleDao;
  private final EmployeeDao employeeDao;
  private final TimesheetDao timesheetDao;

  public List<SalesReportEntry> salesReport(AuthenticatedUser caller, ReportRequest req) {
    requireReportAccess(caller);
    req.validate();

    LocalDateTime dtFrom = req.getFrom().atStartOfDay();
    LocalDateTime dtTo = req.getTo().atTime(LocalTime.MAX);

    List<SalesReportEntry> result = new ArrayList<>();
    for (Store store : storeDao.findAllActive()) {
      List<Sale> sales = saleDao.findByStoreIdAndPeriod(store.getId(), dtFrom, dtTo);

      long salesCount = sales.stream().filter(s -> !s.getIsReturn()).count();
      long returnsCount = sales.stream().filter(Sale::getIsReturn).count();

      BigDecimal gross =
          sales.stream()
              .filter(s -> !s.getIsReturn())
              .map(Sale::getTotal)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal returnsTotal =
          sales.stream()
              .filter(Sale::getIsReturn)
              .map(s -> s.getTotal().abs())
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      result.add(
          new SalesReportEntry(
              store.getId(),
              store.getName(),
              salesCount,
              returnsCount,
              gross,
              gross.subtract(returnsTotal)));
    }
    return result;
  }

  public List<EmployeeStats> employeeEfficiency(AuthenticatedUser caller, ReportRequest req) {
    requireReportAccess(caller);
    req.validate();
    return employeeDao.findActive().stream()
        .map(e -> buildEmployeeStats(e, req.getFrom(), req.getTo()))
        .toList();
  }

  public EmployeeStats employeeEfficiency(
      AuthenticatedUser caller, UUID employeeId, ReportRequest req) {
    requireReportAccess(caller);
    req.validate();
    Employee emp =
        employeeDao
            .findById(employeeId)
            .orElseThrow(() -> new ServiceException("Сотрудник не найден", "EMPLOYEE_NOT_FOUND"));
    return buildEmployeeStats(emp, req.getFrom(), req.getTo());
  }

  public List<StoreEfficiencyReport> storeEfficiency(AuthenticatedUser caller, ReportRequest req) {
    requireReportAccess(caller);
    req.validate();

    LocalDateTime dtFrom = req.getFrom().atStartOfDay();
    LocalDateTime dtTo = req.getTo().atTime(LocalTime.MAX);

    List<StoreEfficiencyReport> result = new ArrayList<>();
    for (Store store : storeDao.findAllActive()) {
      List<Sale> sales = saleDao.findByStoreIdAndPeriod(store.getId(), dtFrom, dtTo);

      // total уже отрицательный для возвратов
      BigDecimal netRevenue =
          sales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

      List<EmployeeStats> empStats =
          employeeDao.findByStoreId(store.getId()).stream()
              .map(e -> buildEmployeeStats(e, req.getFrom(), req.getTo()))
              .toList();

      BigDecimal totalLaborCost =
          empStats.stream()
              .map(EmployeeStats::totalLaborCost)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal totalHours =
          empStats.stream()
              .map(EmployeeStats::totalHoursWorked)
              .reduce(BigDecimal.ZERO, BigDecimal::add);

      BigDecimal revenuePerHour =
          totalHours.compareTo(BigDecimal.ZERO) > 0
              ? netRevenue.divide(totalHours, 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;

      result.add(
          new StoreEfficiencyReport(
              store.getId(),
              store.getName(),
              netRevenue,
              sales.stream().filter(s -> !s.getIsReturn()).count(),
              totalLaborCost,
              totalHours,
              revenuePerHour,
              empStats));
    }
    return result;
  }

  private EmployeeStats buildEmployeeStats(Employee emp, LocalDate from, LocalDate to) {
    List<Timesheet> sheets = timesheetDao.findByEmployeeIdAndPeriod(emp.getId(), from, to);

    BigDecimal totalHours =
        sheets.stream()
            .map(t -> t.getHoursWorked() != null ? t.getHoursWorked() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    int workdays =
        (int)
            sheets.stream()
                .filter(
                    t ->
                        t.getHoursWorked() != null
                            && t.getHoursWorked().compareTo(BigDecimal.ZERO) > 0)
                .count();

    BigDecimal avgHours =
        workdays > 0
            ? totalHours.divide(BigDecimal.valueOf(workdays), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    BigDecimal laborCost =
        totalHours.multiply(emp.getHourlyRate()).setScale(2, RoundingMode.HALF_UP);

    List<Sale> cashierSales =
        saleDao.findByCashierId(emp.getId()).stream()
            .filter(s -> !s.getIsReturn())
            .filter(
                s -> {
                  LocalDate d = s.getSoldAt().toLocalDate();
                  return !d.isBefore(from) && !d.isAfter(to);
                })
            .toList();

    BigDecimal salesRevenue =
        cashierSales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal efficiencyIndex =
        (!cashierSales.isEmpty() && laborCost.compareTo(BigDecimal.ZERO) > 0)
            ? salesRevenue.divide(laborCost, 2, RoundingMode.HALF_UP)
            : null;

    return new EmployeeStats(
        emp.getId(),
        emp.getFullName(),
        emp.getPosition(),
        emp.getStore().getId(),
        emp.getStore().getName(),
        totalHours,
        workdays,
        avgHours,
        emp.getHourlyRate(),
        laborCost,
        cashierSales.size(),
        salesRevenue,
        efficiencyIndex);
  }

  private void requireReportAccess(AuthenticatedUser caller) {
    if (!caller.isAccountant() && !caller.isAdmin())
      throw new AccessDeniedException("Отчёты доступны только ACCOUNTANT и ADMIN");
  }
}
