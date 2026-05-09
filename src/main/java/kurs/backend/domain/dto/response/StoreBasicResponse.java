package kurs.backend.domain.dto.response;

import java.util.UUID;

import lombok.Builder;
import lombok.Data;

import kurs.backend.domain.persistence.entity.Store;

@Data
@Builder
public class StoreBasicResponse {
  private UUID id;
  private String name;

  public static StoreBasicResponse from(Store store) {
    return StoreBasicResponse.builder().id(store.getId()).name(store.getName()).build();
  }
}
