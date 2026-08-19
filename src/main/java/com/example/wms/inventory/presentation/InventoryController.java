package com.example.wms.inventory.presentation;

import com.example.wms.inventory.application.InventoryUseCases;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryUseCases useCases;

    public InventoryController(InventoryUseCases useCases) {
        this.useCases = useCases;
    }

    @PostMapping("/receipts")
    public ResponseEntity<InventoryUseCases.MovementView> receive(@Valid @RequestBody StockRequest request) {
        var movement = useCases.receive(request.toCommand());
        return ResponseEntity.created(URI.create("/api/inventory/movements/" + movement.id())).body(movement);
    }

    @PostMapping("/issues")
    public ResponseEntity<InventoryUseCases.MovementView> issue(@Valid @RequestBody StockRequest request) {
        var movement = useCases.issue(request.toCommand());
        return ResponseEntity.created(URI.create("/api/inventory/movements/" + movement.id())).body(movement);
    }

    @PostMapping("/reservations")
    public ResponseEntity<InventoryUseCases.ReservationView> reserve(@Valid @RequestBody ReserveRequest request) {
        var reservation = useCases.reserve(new InventoryUseCases.ReserveCommand(
                request.warehouseId(), request.productId(), request.quantity(), request.externalReference()));
        return ResponseEntity.created(URI.create("/api/inventory/reservations/" + reservation.id())).body(reservation);
    }

    @DeleteMapping("/reservations/{id}")
    public InventoryUseCases.ReservationView release(@PathVariable UUID id,
                                                      @RequestParam @NotBlank @Size(max = 100) String reference) {
        return useCases.release(id, reference);
    }

    @PostMapping("/transfers")
    public InventoryUseCases.TransferView transfer(@Valid @RequestBody TransferRequest request) {
        return useCases.transfer(new InventoryUseCases.TransferCommand(request.sourceWarehouseId(),
                request.destinationWarehouseId(), request.productId(), request.quantity(), request.externalReference()));
    }

    @GetMapping("/balances/{warehouseId}/{productId}")
    public InventoryUseCases.BalanceView balance(@PathVariable UUID warehouseId, @PathVariable UUID productId) {
        return useCases.balance(warehouseId, productId);
    }

    @GetMapping("/movements")
    public InventoryUseCases.MovementPageView movements(
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return useCases.movements(warehouseId, productId, page, size);
    }

    public record StockRequest(@NotNull UUID warehouseId, @NotNull UUID productId, @Positive long quantity,
                               @NotBlank @Size(max = 100) String externalReference) {
        InventoryUseCases.StockCommand toCommand() {
            return new InventoryUseCases.StockCommand(warehouseId, productId, quantity, externalReference);
        }
    }

    public record ReserveRequest(@NotNull UUID warehouseId, @NotNull UUID productId, @Positive long quantity,
                                 @NotBlank @Size(max = 100) String externalReference) {}

    public record TransferRequest(@NotNull UUID sourceWarehouseId, @NotNull UUID destinationWarehouseId,
                                  @NotNull UUID productId, @Positive long quantity,
                                  @NotBlank @Size(max = 100) String externalReference) {}
}
