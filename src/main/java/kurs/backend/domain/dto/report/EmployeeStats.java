package kurs.backend.domain.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record EmployeeStats(
    UUID employeeId,
    String fullName,
    String position,
    UUID storeId,
    String storeName,
    BigDecimal totalHoursWorked,
    int workdaysCount,
    BigDecimal avgHoursPerDay,
    BigDecimal hourlyRate,
    BigDecimal totalLaborCost,

    /** salesRevenue / totalLaborCost; null если не кассир или нет данных. */
    long salesCount, // > 0 только для кассиров
    BigDecimal salesRevenue,
    BigDecimal efficiencyIndex) {}
