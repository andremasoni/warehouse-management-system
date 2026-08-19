package com.example.wms.warehouse.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class Warehouse {

    private final UUID id;
    private final String code;
    private final String name;
    private final boolean active;

    private Warehouse(UUID id, String code, String name, boolean active) {
        this.id = Objects.requireNonNull(id, "Warehouse id is required");
        if (code == null || !code.trim().toUpperCase(Locale.ROOT).matches("[A-Z0-9_-]{2,30}")) {
            throw new IllegalArgumentException("Warehouse code must have 2 to 30 letters, numbers, hyphens or underscores");
        }
        if (name == null || name.isBlank() || name.trim().length() > 120) {
            throw new IllegalArgumentException("Warehouse name must have 1 to 120 characters");
        }
        this.code = code.trim().toUpperCase(Locale.ROOT);
        this.name = name.trim();
        this.active = active;
    }

    public static Warehouse create(UUID id, String code, String name) {
        return new Warehouse(id, code, name, true);
    }

    public static Warehouse restore(UUID id, String code, String name, boolean active) {
        return new Warehouse(id, code, name, active);
    }

    public UUID id() { return id; }
    public String code() { return code; }
    public String name() { return name; }
    public boolean active() { return active; }
}
