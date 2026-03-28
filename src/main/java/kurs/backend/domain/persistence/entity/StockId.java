package kurs.backend.domain.persistence.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StockId implements Serializable {
    private UUID storageLocation;
    private UUID product;
}
