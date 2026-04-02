package kurs.backend.domain.dto.responce;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Статистика и эффективность сотрудника за период.
 *
 * <p>Используется в отчётах по труду и в дальнейшем — для алгоритма оптимального распределения
 * сотрудников по точкам.
 */
public record EmployeeStats(
    UUID employeeId,
    String fullName,
    String position,
    UUID storeId,
    String storeName,

    // Рабочее время
    BigDecimal totalHoursWorked,
    int workdaysCount,
    BigDecimal avgHoursPerDay,

    // Финансы
    BigDecimal hourlyRate,
    BigDecimal totalLaborCost, // totalHoursWorked * hourlyRate

    // Продуктивность (для CASHIER — объём продаж)
    long salesCount,
    BigDecimal salesRevenue,

    /**
     * Индекс эффективности: salesRevenue / totalLaborCost. null, если нет данных о продажах (не
     * кассир).
     */
    BigDecimal efficiencyIndex) {}
