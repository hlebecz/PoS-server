package kurs.backend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.Product;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
  private UUID id;
  private String name;
  private String article;
  private BigDecimal price;
  private LocalDateTime createdAt;

  public static ProductResponse from(Product p) {
    return ProductResponse.builder()
        .id(p.getId())
        .name(p.getName())
        .article(p.getArticle())
        .price(p.getPrice())
        .createdAt(p.getCreatedAt())
        .build();
  }
}
