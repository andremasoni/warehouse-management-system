package com.example.wms.inventory.application;

import com.example.wms.inventory.domain.MovementType;
import com.example.wms.inventory.domain.ReservationStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface InventoryUseCases {
    MovementView receive(StockCommand command);
    MovementView issue(StockCommand command);
    ReservationView reserve(ReserveCommand command);
    ReservationView release(UUID reservationId, String externalReference);
    ReservationView consume(UUID reservationId, String externalReference);
    TransferView transfer(TransferCommand command);
    BalanceView balance(UUID warehouseId, UUID productId);
    MovementPageView movements(UUID warehouseId, UUID productId, int page, int size);

    record StockCommand(UUID warehouseId, UUID productId, long quantity, String externalReference) {}
    record ReserveCommand(UUID warehouseId, UUID productId, long quantity, String externalReference) {}
    record TransferCommand(UUID sourceWarehouseId, UUID destinationWarehouseId, UUID productId,
                           long quantity, String externalReference) {}
    record BalanceView(UUID warehouseId, UUID productId, long onHand, long reserved, long available) {}
    record ReservationView(UUID id, UUID warehouseId, UUID productId, long quantity,
                           String externalReference, ReservationStatus status, Instant createdAt) {}
    record MovementView(UUID id, UUID warehouseId, UUID productId, UUID reservationId,
                        MovementType type, long quantity, String externalReference, Instant occurredAt) {}
    record TransferView(MovementView outgoing, MovementView incoming) {}
    record MovementPageView(List<MovementView> content, int page, int size, long totalElements, int totalPages) {}
}
