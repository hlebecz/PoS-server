package kurs.backend.domain.dto.response;

import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.Warehouse;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
  private UUID id;
  private String name;
  private String phone;
  private boolean isActive;
  private LocationResponse location;

  public static WarehouseResponse from(Warehouse w) {
    return WarehouseResponse.builder()
        .id(w.getId())
        .name(w.getName())
        .phone(w.getPhone())
        .isActive(w.getIsActive())
        .location(LocationResponse.from(w.getLocation()))
        .build();
  }
}
