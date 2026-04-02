package kurs.backend.domain.dto.report;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StoreEfficiencyReport(
    UUID storeId,
    String storeName,
    BigDecimal netRevenue,
    long salesCount,
    BigDecimal totalLaborCost,
    BigDecimal totalHoursWorked,
    BigDecimal revenuePerHour,
    List<EmployeeStats> employeeStats) {}
