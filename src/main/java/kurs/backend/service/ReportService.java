package kurs.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kurs.backend.domain.dto.responce.EmployeeStats;
import kurs.backend.domain.dto.responce.SalesReportEntry;
import kurs.backend.domain.dto.responce.StoreEfficiencyReport;
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

/**
 * Генерация аналитических отчётов.
 *
 * <p>Доступ: ACCOUNTANT и ADMIN.
 *
 * <ul>
 *   <li>{@link #salesReport} — сводка по продажам и возвратам в разрезе точек.
 *   <li>{@link #employeeEfficiency} — KPI каждого сотрудника за период.
 *   <li>{@link #storeEfficiency} — KPI по точкам со списком сотрудников.
 * </ul>
 */
public class ReportService {

  private final StoreDao storeDao;
  private final SaleDao saleDao;
  private final EmployeeDao employeeDao;
  private final TimesheetDao timesheetDao;

  public ReportService(
      StoreDao storeDao, SaleDao saleDao, EmployeeDao employeeDao, TimesheetDao timesheetDao) {
    this.storeDao = storeDao;
    this.saleDao = saleDao;
    this.employeeDao = employeeDao;
    this.timesheetDao = timesheetDao;
  }

  // -----------------------------------------------------------------------
  // Отчёт о продажах
  // -----------------------------------------------------------------------

  /** Возвращает агрегированный отчёт о продажах по всем активным точкам за период. */
  public List<SalesReportEntry> salesReport(
      AuthenticatedUser caller, LocalDate from, LocalDate to) {
    requireReportAccess(caller);

    LocalDateTime dtFrom = from.atStartOfDay();
    LocalDateTime dtTo = to.atTime(LocalTime.MAX);

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
              .map(Sale::getTotal)
              .map(BigDecimal::abs)
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

  // -----------------------------------------------------------------------
  // Эффективность сотрудников
  // -----------------------------------------------------------------------

  /**
   * KPI по каждому сотруднику за период. Для кассиров дополнительно считается efficiencyIndex
   * (выручка / затраты на труд).
   */
  public List<EmployeeStats> employeeEfficiency(
      AuthenticatedUser caller, LocalDate from, LocalDate to) {
    requireReportAccess(caller);

    List<EmployeeStats> result = new ArrayList<>();

    for (Employee emp : employeeDao.findActive()) {
      result.add(buildEmployeeStats(emp, from, to));
    }

    return result;
  }

  /** KPI конкретного сотрудника. */
  public EmployeeStats employeeEfficiency(
      AuthenticatedUser caller, UUID employeeId, LocalDate from, LocalDate to) {
    requireReportAccess(caller);
    Employee emp =
        employeeDao
            .findById(employeeId)
            .orElseThrow(() -> new ServiceException("Сотрудник не найден", "EMPLOYEE_NOT_FOUND"));
    return buildEmployeeStats(emp, from, to);
  }

  // -----------------------------------------------------------------------
  // Эффективность по точкам
  // -----------------------------------------------------------------------

  /** Сводный отчёт по каждой активной точке: выручка, затраты на труд, KPI. */
  public List<StoreEfficiencyReport> storeEfficiency(
      AuthenticatedUser caller, LocalDate from, LocalDate to) {
    requireReportAccess(caller);

    LocalDateTime dtFrom = from.atStartOfDay();
    LocalDateTime dtTo = to.atTime(LocalTime.MAX);

    List<StoreEfficiencyReport> result = new ArrayList<>();

    for (Store store : storeDao.findAllActive()) {
      List<Sale> sales = saleDao.findByStoreIdAndPeriod(store.getId(), dtFrom, dtTo);
      List<Employee> employees = employeeDao.findByStoreId(store.getId());

      BigDecimal netRevenue =
          sales.stream()
              .map(s -> s.getIsReturn() ? s.getTotal() : s.getTotal())
              .reduce(BigDecimal.ZERO, BigDecimal::add); // возвраты уже отрицательные

      List<EmployeeStats> empStats =
          employees.stream().map(e -> buildEmployeeStats(e, from, to)).toList();

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

  // -----------------------------------------------------------------------
  // Helpers
  // -----------------------------------------------------------------------

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

    // Продажи — только для кассиров
    List<Sale> cashierSales =
        saleDao.findByCashierId(emp.getId()).stream()
            .filter(s -> !s.getIsReturn())
            .filter(
                s ->
                    !s.getSoldAt().toLocalDate().isBefore(from)
                        && !s.getSoldAt().toLocalDate().isAfter(to))
            .toList();

    long salesCount = cashierSales.size();
    BigDecimal salesRevenue =
        cashierSales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal efficiencyIndex = null;
    if (salesCount > 0 && laborCost.compareTo(BigDecimal.ZERO) > 0) {
      efficiencyIndex = salesRevenue.divide(laborCost, 2, RoundingMode.HALF_UP);
    }

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
        salesCount,
        salesRevenue,
        efficiencyIndex);
  }

  private void requireReportAccess(AuthenticatedUser caller) {
    if (!caller.isAccountant() && !caller.isAdmin()) {
      throw new AccessDeniedException("Отчёты доступны только ACCOUNTANT и ADMIN");
    }
  }
}
