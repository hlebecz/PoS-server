package kurs.backend.domain.persistence.entity;

import java.util.List;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "warehouses")
@PrimaryKeyJoinColumn(name = "id")
@Getter
@Setter
@NoArgsConstructor
public class Warehouse extends StorageLocation {

  @Column(length = 20)
  private String phone;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_id")
  private Location location;

  @OneToMany(mappedBy = "warehouse", fetch = FetchType.LAZY)
  private List<Store> stores;
}
