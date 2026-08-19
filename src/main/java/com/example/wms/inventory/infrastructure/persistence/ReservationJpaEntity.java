package com.example.wms.inventory.infrastructure.persistence;

import com.example.wms.inventory.domain.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_reservations")
public class ReservationJpaEntity {
    @Id private UUID id;
    @Column(name = "warehouse_id", nullable = false) private UUID warehouseId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(nullable = false) private long quantity;
    @Column(name = "external_reference", nullable = false, unique = true, length = 100) private String externalReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReservationStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected ReservationJpaEntity() {}

    public ReservationJpaEntity(UUID id, UUID warehouseId, UUID productId, long quantity, String externalReference,
                                ReservationStatus status, Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.productId = productId;
        this.quantity = quantity;
        this.externalReference = externalReference;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() { return id; }
    public UUID getWarehouseId() { return warehouseId; }
    public UUID getProductId() { return productId; }
    public long getQuantity() { return quantity; }
    public String getExternalReference() { return externalReference; }
    public ReservationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
