package kurs.backend.domain.dto.request;

import java.time.LocalDate;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest extends BaseRequest {
  private LocalDate from;
  private LocalDate to;

  @Override
  public void validate() {
    require(from, "from");
    require(to, "to");
    if (from.isAfter(to)) throw new IllegalArgumentException("from не может быть позже to");
  }
}
