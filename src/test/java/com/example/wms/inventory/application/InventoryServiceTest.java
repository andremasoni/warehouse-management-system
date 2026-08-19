package com.example.wms.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.wms.inventory.domain.MovementType;
import com.example.wms.inventory.domain.StockBalance;
import com.example.wms.inventory.domain.StockMovement;
import com.example.wms.product.application.ProductUseCases;
import com.example.wms.shared.application.AccessControl;
import com.example.wms.shared.application.TransactionRunner;
import com.example.wms.shared.domain.ConflictException;
import com.example.wms.warehouse.application.WarehouseUseCases;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventory;
    @Mock ReservationRepository reservations;
    @Mock MovementRepository movements;
    @Mock ProductUseCases products;
    @Mock WarehouseUseCases warehouses;
    @Mock AccessControl accessControl;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        TransactionRunner transactions = new TransactionRunner() {
            @Override
            public <T> T required(Supplier<T> operation) {
                return operation.get();
            }
        };
        service = new InventoryService(inventory, reservations, movements, products, warehouses,
                transactions, accessControl, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void receiptCoordinatesLockBalanceAndAuditMovement() {
        var warehouseId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var balance = StockBalance.empty(UUID.randomUUID(), warehouseId, productId);
        when(warehouses.existsActive(warehouseId)).thenReturn(true);
        when(products.existsActive(productId)).thenReturn(true);
        when(movements.findByExternalReference("receipt-1")).thenReturn(Optional.empty());
        when(inventory.lockOrCreate(warehouseId, productId)).thenReturn(balance);
        when(inventory.save(balance)).thenReturn(balance);
        when(movements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.receive(new InventoryUseCases.StockCommand(
                warehouseId, productId, 5, "receipt-1"));

        assertThat(balance.onHand()).isEqualTo(5);
        assertThat(result.type()).isEqualTo(MovementType.RECEIPT);
        assertThat(result.occurredAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        verify(inventory).save(balance);
        verify(movements).save(any(StockMovement.class));
    }

    @Test
    void reusedIdempotencyReferenceWithDifferentPayloadIsRejectedBeforeMutation() {
        var warehouseId = UUID.randomUUID();
        var productId = UUID.randomUUID();
        var existing = new StockMovement(UUID.randomUUID(), warehouseId, productId, null,
                MovementType.RECEIPT, 5, "receipt-1", Instant.now());
        when(warehouses.existsActive(warehouseId)).thenReturn(true);
        when(products.existsActive(productId)).thenReturn(true);
        when(movements.findByExternalReference("receipt-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.receive(new InventoryUseCases.StockCommand(
                warehouseId, productId, 6, "receipt-1")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("different operation data");

        verify(inventory, never()).lockOrCreate(any(), any());
    }
}
