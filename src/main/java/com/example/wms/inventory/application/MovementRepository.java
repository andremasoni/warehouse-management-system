package com.example.wms.inventory.application;

import com.example.wms.inventory.domain.StockMovement;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovementRepository {
    StockMovement save(StockMovement movement);
    Optional<StockMovement> findByExternalReference(String externalReference);
    MovementPage find(UUID warehouseId, UUID productId, int page, int size);

    record MovementPage(List<StockMovement> content, long totalElements, int totalPages) {}
}
