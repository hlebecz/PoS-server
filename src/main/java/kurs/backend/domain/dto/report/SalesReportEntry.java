package kurs.backend.domain.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record SalesReportEntry(
    UUID storeId,
    String storeName,
    long totalSalesCount,
    long totalReturnsCount,
    BigDecimal grossRevenue,
    BigDecimal netRevenue) {}
