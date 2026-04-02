package kurs.backend.domain.dto.response;

import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.Store;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
  private UUID id;
  private String name;
  private String phone;
  private boolean isActive;
  private UUID managerId;
  private String managerLogin;
  private UUID warehouseId;
  private String warehouseName;
  private LocationResponse location;

  public static StoreResponse from(Store s) {
    return StoreResponse.builder()
        .id(s.getId())
        .name(s.getName())
        .phone(s.getPhone())
        .isActive(s.getIsActive())
        .managerId(s.getManager() != null ? s.getManager().getId() : null)
        .managerLogin(s.getManager() != null ? s.getManager().getLogin() : null)
        .warehouseId(s.getWarehouse() != null ? s.getWarehouse().getId() : null)
        .warehouseName(s.getWarehouse() != null ? s.getWarehouse().getName() : null)
        .location(LocationResponse.from(s.getLocation()))
        .build();
  }
}
