package com.example.wms.warehouse.application;

import java.util.UUID;

public interface WarehouseUseCases {
    WarehouseView create(CreateWarehouseCommand command);
    WarehouseView get(UUID id);
    boolean existsActive(UUID id);

    record CreateWarehouseCommand(String code, String name) {}
    record WarehouseView(UUID id, String code, String name, boolean active) {}
}
