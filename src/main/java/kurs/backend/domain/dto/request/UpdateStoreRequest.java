package kurs.backend.domain.dto.request;

import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreRequest extends BaseRequest {
  private UUID id;
  private String name;
  private String phone;
  private UUID managerId; // null = снять менеджера
  private UUID warehouseId; // null = отвязать склад
  private UUID locationId; // null = не менять
  private Boolean isActive; // null = не менять

  @Override
  public void validate() {
    require(id, "id");
    requireMaxLength(name, 150, "name");
    requireMaxLength(phone, 150, "phone");
  }
}
