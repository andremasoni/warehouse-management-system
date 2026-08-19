package com.example.wms.warehouse.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "warehouses")
public class WarehouseJpaEntity {
    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 30) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(nullable = false) private boolean active;

    protected WarehouseJpaEntity() {}

    public WarehouseJpaEntity(UUID id, String code, String name, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.active = active;
    }

    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
