package kurs.backend.domain.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;


@Entity
@Table(name = "stores")
@PrimaryKeyJoinColumn(name = "id")
@Getter @Setter
@NoArgsConstructor
public class Store extends StorageLocation {

    @Column(nullable = false, length = 150)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private User manager;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<Employee> employees;

    @OneToMany(mappedBy = "store", fetch = FetchType.LAZY)
    private List<Sale> sales;
}
