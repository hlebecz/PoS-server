package kurs.backend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.*;

import kurs.backend.domain.persistence.entity.Timesheet;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetResponse {
  private UUID id;
  private UUID employeeId;
  private String employeeFullName;
  private UUID storeId;
  private String storeName;
  private LocalDate workDate;
  private LocalTime checkIn;
  private LocalTime checkOut;
  private BigDecimal hoursWorked;

  public static TimesheetResponse from(Timesheet t) {
    return TimesheetResponse.builder()
        .id(t.getId())
        .employeeId(t.getEmployee().getId())
        .employeeFullName(t.getEmployee().getFullName())
        .storeId(t.getEmployee().getStore().getId())
        .storeName(t.getEmployee().getStore().getName())
        .workDate(t.getWorkDate())
        .checkIn(t.getCheckIn())
        .checkOut(t.getCheckOut())
        .hoursWorked(t.getHoursWorked())
        .build();
  }
}
