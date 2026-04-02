package kurs.backend.domain.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.Location;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {
  private UUID id;
  private BigDecimal x;
  private BigDecimal y;
  private String address;
  private String city;

  public static LocationResponse from(Location l) {
    if (l == null) return null;
    return LocationResponse.builder()
        .id(l.getId())
        .x(l.getX())
        .y(l.getY())
        .address(l.getAddress())
        .city(l.getCity())
        .build();
  }
}
