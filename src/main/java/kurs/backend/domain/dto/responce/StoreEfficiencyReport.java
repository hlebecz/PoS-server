package kurs.backend.domain.dto.responce;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Сводный отчёт эффективности торговой точки за период. */
public record StoreEfficiencyReport(
    UUID storeId,
    String storeName,

    // Продажи
    BigDecimal netRevenue,
    long salesCount,

    // Труд
    BigDecimal totalLaborCost,
    BigDecimal totalHoursWorked,

    /** Выручка на рабочий час: netRevenue / totalHoursWorked. Ключевой KPI точки. */
    BigDecimal revenuePerHour,

    /** Детализация по каждому сотруднику точки. */
    List<EmployeeStats> employeeStats) {}
