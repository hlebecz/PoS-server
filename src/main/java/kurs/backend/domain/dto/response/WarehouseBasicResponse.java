package kurs.backend.domain.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import kurs.backend.domain.persistence.entity.Warehouse;

@Data
@Builder
public class WarehouseBasicResponse {
  private UUID id;
  private String name;

  public static WarehouseBasicResponse from(Warehouse warehouse) {
    return WarehouseBasicResponse.builder().id(warehouse.getId()).name(warehouse.getName()).build();
  }
}
