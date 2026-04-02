package kurs.backend.domain.dto.request;

import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetStockRequest extends BaseRequest {
  private UUID storageLocationId;
  private UUID productId;
  private int quantity;

  @Override
  public void validate() {
    require(storageLocationId, "storageLocationId");
    require(productId, "productId");
    if (quantity < 0) throw new IllegalArgumentException("quantity не может быть отрицательным");
  }
}
