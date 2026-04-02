package kurs.backend.domain.dto.request;

import java.util.List;
import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSaleRequest extends BaseRequest {
  private List<SaleItemRequest> items;

  @Override
  public void validate() {
    if (items == null || items.isEmpty())
      throw new IllegalArgumentException("Список позиций не может быть пустым");
    items.forEach(SaleItemRequest::validate);
  }

  @Getter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SaleItemRequest extends BaseRequest {
    private UUID productId;
    private int quantity;

    @Override
    public void validate() {
      require(productId, "productId");
      if (quantity <= 0) throw new IllegalArgumentException("quantity должен быть больше нуля");
    }
  }
}
