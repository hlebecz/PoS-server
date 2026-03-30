package kurs.backend.domain.persistence.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "storage_locations")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class StorageLocation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @Column(length = 150)
  private String name;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "storageLocation", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Stock> stock;
}
