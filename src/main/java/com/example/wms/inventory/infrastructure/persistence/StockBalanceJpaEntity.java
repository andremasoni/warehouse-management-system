package com.example.wms.inventory.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "stock_balances", uniqueConstraints =
        @UniqueConstraint(name = "uk_stock_balance_warehouse_product", columnNames = {"warehouse_id", "product_id"}))
public class StockBalanceJpaEntity {
    @Id private UUID id;
    @Column(name = "warehouse_id", nullable = false) private UUID warehouseId;
    @Column(name = "product_id", nullable = false) private UUID productId;
    @Column(name = "on_hand", nullable = false) private long onHand;
    @Column(nullable = false) private long reserved;
    @Version @Column(nullable = false) private long version;

    protected StockBalanceJpaEntity() {}

    public StockBalanceJpaEntity(UUID id, UUID warehouseId, UUID productId, long onHand, long reserved, long version) {
        this.id = id;
        this.warehouseId = warehouseId;
        this.productId = productId;
        this.onHand = onHand;
        this.reserved = reserved;
        this.version = version;
    }

    public UUID getId() { return id; }
    public UUID getWarehouseId() { return warehouseId; }
    public UUID getProductId() { return productId; }
    public long getOnHand() { return onHand; }
    public long getReserved() { return reserved; }
    public long getVersion() { return version; }
}
