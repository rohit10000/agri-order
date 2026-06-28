package in.agri.order.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "stock")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_item", nullable = false, unique = true, columnDefinition = "product_item")
    private ProductItem productItem;

    @DecimalMin(value = "0.0")
    @Column(name = "available_kg", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal availableKg = BigDecimal.ZERO;

    @DecimalMin(value = "0.0")
    @Column(name = "reserved_kg", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal reservedKg = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    public BigDecimal getFreeStock() {
        return availableKg.subtract(reservedKg);
    }

    public void reserveStock(BigDecimal quantity) {
        if (getFreeStock().compareTo(quantity) < 0) {
            throw new IllegalStateException("Insufficient stock to reserve");
        }
        this.reservedKg = this.reservedKg.add(quantity);
    }

    public void releaseStock(BigDecimal quantity) {
        this.reservedKg = this.reservedKg.subtract(quantity);
        if (this.reservedKg.compareTo(BigDecimal.ZERO) < 0) {
            this.reservedKg = BigDecimal.ZERO;
        }
    }
}
