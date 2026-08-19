package com.example.wms.product.domain;

import java.util.Objects;
import java.util.UUID;

public final class Product {

    private final UUID id;
    private final Sku sku;
    private String name;
    private boolean active;

    private Product(UUID id, Sku sku, String name, boolean active) {
        this.id = Objects.requireNonNull(id, "Product id is required");
        this.sku = Objects.requireNonNull(sku, "SKU is required");
        rename(name);
        this.active = active;
    }

    public static Product create(UUID id, Sku sku, String name) {
        return new Product(id, sku, name, true);
    }

    public static Product restore(UUID id, Sku sku, String name, boolean active) {
        return new Product(id, sku, name, active);
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank() || newName.trim().length() > 160) {
            throw new IllegalArgumentException("Product name must have 1 to 160 characters");
        }
        this.name = newName.trim();
    }

    public void deactivate() {
        this.active = false;
    }

    public UUID id() { return id; }
    public Sku sku() { return sku; }
    public String name() { return name; }
    public boolean active() { return active; }
}
