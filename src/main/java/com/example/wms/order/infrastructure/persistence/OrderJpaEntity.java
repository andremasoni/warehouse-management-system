package com.example.wms.order.infrastructure.persistence;

import com.example.wms.order.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {
    @Id private UUID id;
    @Column(name = "warehouse_id", nullable = false) private UUID warehouseId;
    @Column(name = "external_reference", nullable = false, unique = true, length = 100) private String externalReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OrderStatus status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItemJpaEntity> items = new ArrayList<>();

    protected OrderJpaEntity() {}

    public OrderJpaEntity(UUID id, UUID warehouseId, String externalReference, OrderStatus status,
                          Instant createdAt, Instant updatedAt, long version) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.externalReference = externalReference;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public void addItem(OrderItemJpaEntity item) {
        items.add(item);
        item.attach(this);
    }

    public UUID getId() { return id; }
    public UUID getWarehouseId() { return warehouseId; }
    public String getExternalReference() { return externalReference; }
    public OrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
    public List<OrderItemJpaEntity> getItems() { return items; }
}
