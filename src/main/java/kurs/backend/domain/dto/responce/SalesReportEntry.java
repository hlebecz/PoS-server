package kurs.backend.domain.dto.responce;

import java.math.BigDecimal;
import java.util.UUID;

/** Агрегированная строка отчёта о продажах по точке за период. */
public record SalesReportEntry(
    UUID storeId,
    String storeName,
    long totalSalesCount,
    long totalReturnsCount,
    BigDecimal grossRevenue, // сумма продаж без вычета возвратов
    BigDecimal netRevenue // grossRevenue - сумма возвратов
    ) {}
