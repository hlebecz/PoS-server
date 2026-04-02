package kurs.backend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.Stock;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResponse {
  private UUID storageLocationId;
  private String storageLocationName;
  private UUID productId;
  private String productName;
  private String productArticle;
  private BigDecimal productPrice;
  private int quantity;
  private LocalDateTime updatedAt;

  public static StockResponse from(Stock s) {
    return StockResponse.builder()
        .storageLocationId(s.getStorageLocation().getId())
        .storageLocationName(s.getStorageLocation().getName())
        .productId(s.getProduct().getId())
        .productName(s.getProduct().getName())
        .productArticle(s.getProduct().getArticle())
        .productPrice(s.getProduct().getPrice())
        .quantity(s.getQuantity())
        .updatedAt(s.getUpdatedAt())
        .build();
  }
}
