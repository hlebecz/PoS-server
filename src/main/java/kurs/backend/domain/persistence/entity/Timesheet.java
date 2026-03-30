package kurs.backend.domain.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(
    name = "timesheets",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_timesheet_employee_date",
            columnNames = {"employee_id", "work_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Timesheet {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "employee_id", nullable = false)
  private Employee employee;

  @Column(name = "work_date", nullable = false)
  private LocalDate workDate;

  @Column(name = "check_in")
  private LocalTime checkIn;

  @Column(name = "check_out")
  private LocalTime checkOut;

  @Column(name = "hours_worked", precision = 4, scale = 2)
  private BigDecimal hoursWorked;
}
