package com.example.wms.inventory.infrastructure.persistence;

import com.example.wms.inventory.domain.MovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements")
public class StockMovementJpaEntity {
    @Id private UUID id;
    @Column(name = "warehouse_id", nullable = false) private UUID warehouseId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "reservation_id") private UUID reservationId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private MovementType type;
    @Column(nullable = false) private long quantity;
    @Column(name = "external_reference", nullable = false, unique = true, length = 120) private String externalReference;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;

    protected StockMovementJpaEntity() {}

    public StockMovementJpaEntity(UUID id, UUID warehouseId, UUID productId, UUID reservationId, MovementType type,
                                  long quantity, String externalReference, Instant occurredAt) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.productId = productId;
        this.reservationId = reservationId;
        this.type = type;
        this.quantity = quantity;
        this.externalReference = externalReference;
        this.occurredAt = occurredAt;
    }

    public UUID getId() { return id; }
    public UUID getWarehouseId() { return warehouseId; }
    public UUID getProductId() { return productId; }
    public UUID getReservationId() { return reservationId; }
    public MovementType getType() { return type; }
    public long getQuantity() { return quantity; }
    public String getExternalReference() { return externalReference; }
    public Instant getOccurredAt() { return occurredAt; }
}
