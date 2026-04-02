package kurs.backend.domain.dto.request;

import java.util.UUID;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWarehouseRequest extends BaseRequest {
  private UUID id;
  private String name;
  private String phone;
  private UUID locationId;
  private Boolean isActive;

  @Override
  public void validate() {
    require(id, "id");
    requireMaxLength(name, 150, "name");
    requireMaxLength(phone, 20, "phone");
  }
}
