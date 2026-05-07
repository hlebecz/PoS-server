package kurs.backend.domain.dto.request;

import java.math.BigDecimal;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest extends BaseRequest {
  private String name;
  private String article;
  private BigDecimal price;

  @Override
  public void validate() {
    requireNotBlank(name, "name");
    requireMaxLength(name, 200, "name");
    requireNotBlank(article, "article");
    requireMaxLength(article, 100, "article");
    require(price, "price");
    if (price.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Цена не может быть отрицательной");
    }
  }
}
