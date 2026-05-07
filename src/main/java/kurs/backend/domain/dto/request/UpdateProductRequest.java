package kurs.backend.domain.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductRequest extends BaseRequest {
  private UUID id;
  private String name;
  private String article;
  private BigDecimal price;

  @Override
  public void validate() {
    require(id, "id");
    if (name != null) {
      requireMaxLength(name, 200, "name");
    }
    if (article != null) {
      requireMaxLength(article, 100, "article");
    }
    if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Цена не может быть отрицательной");
    }
  }
}
