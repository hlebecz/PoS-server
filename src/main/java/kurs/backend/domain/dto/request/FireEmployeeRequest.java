package kurs.backend.domain.dto.request;

import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FireEmployeeRequest extends BaseRequest {
  UUID id;
  LocalDate firedAt;

  @Override
  public void validate() {
    require(id, "id");
    require(firedAt, "firedAt");
  }
}
