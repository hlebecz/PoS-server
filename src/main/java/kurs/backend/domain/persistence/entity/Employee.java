package kurs.backend.domain.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(updatable = false, nullable = false)
  private UUID id;

  // 1:1 — сотрудник может иметь учётную запись
  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", unique = true)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "store_id", nullable = false)
  private Store store;

  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  @Column(nullable = false, length = 100)
  private String position;

  @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
  private BigDecimal hourlyRate;

  @Column(length = 20)
  private String phone;

  @Column(length = 100)
  private String email;

  @Column(name = "hired_at", nullable = false)
  private LocalDate hiredAt;

  @Column(name = "fired_at")
  private LocalDate firedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "location_id")
  private Location location;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Табель рабочего времени сотрудника
  @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Timesheet> timesheets;
}
