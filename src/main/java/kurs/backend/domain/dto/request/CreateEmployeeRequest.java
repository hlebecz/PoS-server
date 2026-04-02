package kurs.backend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest extends BaseRequest {
  private UUID storeId;
  private UUID userId; // опционально
  private UUID locationId; // опционально
  private String fullName;
  private String position;
  private BigDecimal hourlyRate;
  private String phone;
  private String email;
  private LocalDate hiredAt;

  @Override
  public void validate() {
    require(storeId, "storeId");
    requireNotBlank(fullName, "fullName");
    requireMaxLength(fullName, 200, "fullName");
    requireNotBlank(position, "position");
    requireMaxLength(position, 100, "position");
    require(hourlyRate, "hourlyRate");
    if (hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) < 0)
      throw new IllegalArgumentException("hourlyRate не может быть отрицательным");
    require(hiredAt, "hiredAt");
    requireMaxLength(phone, 20, "phone");
    requireMaxLength(email, 100, "email");
  }
}
