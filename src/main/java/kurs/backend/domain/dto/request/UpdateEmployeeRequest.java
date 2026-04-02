package kurs.backend.domain.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest extends BaseRequest {
  private UUID id;
  private UUID storeId; // null = не переводить
  private UUID locationId; // null = не менять
  private String fullName;
  private String position;
  private BigDecimal hourlyRate;
  private String phone;
  private String email;
  private LocalDate firedAt; // null = не увольнять

  @Override
  public void validate() {
    require(id, "id");
    requireMaxLength(fullName, 200, "fullName");
    requireMaxLength(position, 100, "position");
    requireMaxLength(phone, 20, "phone");
    requireMaxLength(email, 100, "email");
    if (hourlyRate != null && hourlyRate.compareTo(BigDecimal.ZERO) < 0)
      throw new IllegalArgumentException("hourlyRate не может быть отрицательным");
  }
}
