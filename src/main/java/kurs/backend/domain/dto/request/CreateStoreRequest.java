package kurs.backend.domain.dto.request;

import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoreRequest extends BaseRequest {
  private String name;
  private String phone;
  private UUID managerId; // опционально
  private UUID warehouseId; // опционально
  private UUID locationId; // опционально

  @Override
  public void validate() {
    requireNotBlank(name, "name");
    requireMaxLength(name, 150, "name");
    requireNotBlank(phone, "phone");
    requireMaxLength(phone, 150, "phone");
  }
}
