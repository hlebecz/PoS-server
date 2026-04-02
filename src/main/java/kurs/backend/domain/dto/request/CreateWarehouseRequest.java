package kurs.backend.domain.dto.request;

import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWarehouseRequest extends BaseRequest {
  private String name;
  private String phone;
  private UUID locationId; // опционально

  @Override
  public void validate() {
    requireNotBlank(name, "name");
    requireMaxLength(name, 150, "name");
    requireMaxLength(phone, 20, "phone");
  }
}
