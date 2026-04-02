package kurs.backend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.Sale;
import kurs.backend.domain.persistence.entity.SaleItem;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {
  private UUID id;
  private UUID storeId;
  private String storeName;
  private UUID cashierId;
  private String cashierName;
  private BigDecimal total;
  private boolean isReturn;
  private LocalDateTime soldAt;
  private List<SaleItemResponse> items;

  public static SaleResponse from(Sale s) {
    return SaleResponse.builder()
        .id(s.getId())
        .storeId(s.getStore().getId())
        .storeName(s.getStore().getName())
        .cashierId(s.getCashier().getId())
        .cashierName(s.getCashier().getFullName())
        .total(s.getTotal())
        .isReturn(s.getIsReturn())
        .soldAt(s.getSoldAt())
        .items(s.getItems().stream().map(SaleItemResponse::from).toList())
        .build();
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SaleItemResponse {
    private UUID productId;
    private String productName;
    private String productArticle;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public static SaleItemResponse from(SaleItem i) {
      return SaleItemResponse.builder()
          .productId(i.getProduct().getId())
          .productName(i.getProduct().getName())
          .productArticle(i.getProduct().getArticle())
          .quantity(i.getQuantity())
          .unitPrice(i.getUnitPrice())
          .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
          .build();
    }
  }
}
