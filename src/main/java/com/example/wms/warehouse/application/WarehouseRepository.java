package com.example.wms.warehouse.application;

import com.example.wms.warehouse.domain.Warehouse;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findById(UUID id);
    Optional<Warehouse> findByCode(String code);
}
