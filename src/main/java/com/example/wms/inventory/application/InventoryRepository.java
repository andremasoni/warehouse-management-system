package com.example.wms.inventory.application;

import com.example.wms.inventory.domain.StockBalance;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository {
    StockBalance lockOrCreate(UUID warehouseId, UUID productId);
    Optional<StockBalance> find(UUID warehouseId, UUID productId);
    StockBalance save(StockBalance balance);
}
