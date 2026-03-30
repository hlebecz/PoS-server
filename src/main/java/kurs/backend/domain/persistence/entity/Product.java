package kurs.backend.domain.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(nullable = false, unique = true, length = 100)
  private String article;

  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Остатки этого товара по всем хранилищам
  @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
  private List<Stock> stock;
}
