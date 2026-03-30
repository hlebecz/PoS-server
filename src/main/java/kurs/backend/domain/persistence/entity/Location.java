package kurs.backend.domain.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Column(nullable = false, precision = 10, scale = 8)
  private BigDecimal x;

  @Column(nullable = false, precision = 11, scale = 8)
  private BigDecimal y;

  @Column(length = 255)
  private String address;

  @Column(length = 100)
  private String city;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;
}
