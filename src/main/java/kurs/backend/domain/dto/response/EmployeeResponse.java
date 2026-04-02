package kurs.backend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.Employee;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
  private UUID id;
  private UUID storeId;
  private String storeName;
  private UUID userId;
  private String fullName;
  private String position;
  private BigDecimal hourlyRate;
  private String phone;
  private String email;
  private LocalDate hiredAt;
  private LocalDate firedAt;
  private LocationResponse location;
  private LocalDateTime createdAt;

  public static EmployeeResponse from(Employee e) {
    return EmployeeResponse.builder()
        .id(e.getId())
        .storeId(e.getStore().getId())
        .storeName(e.getStore().getName())
        .userId(e.getUser() != null ? e.getUser().getId() : null)
        .fullName(e.getFullName())
        .position(e.getPosition())
        .hourlyRate(e.getHourlyRate())
        .phone(e.getPhone())
        .email(e.getEmail())
        .hiredAt(e.getHiredAt())
        .firedAt(e.getFiredAt())
        .location(LocationResponse.from(e.getLocation()))
        .createdAt(e.getCreatedAt())
        .build();
  }
}
