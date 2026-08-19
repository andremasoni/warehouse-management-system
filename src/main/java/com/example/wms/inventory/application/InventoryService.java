package com.example.wms.inventory.application;

import com.example.wms.inventory.domain.MovementType;
import com.example.wms.inventory.domain.Reservation;
import com.example.wms.inventory.domain.StockBalance;
import com.example.wms.inventory.domain.StockMovement;
import com.example.wms.product.application.ProductUseCases;
import com.example.wms.shared.application.AccessControl;
import com.example.wms.shared.application.Role;
import com.example.wms.shared.application.TransactionRunner;
import com.example.wms.shared.domain.ConflictException;
import com.example.wms.shared.domain.NotFoundException;
import com.example.wms.warehouse.application.WarehouseUseCases;
import java.time.Clock;
import java.util.UUID;

public final class InventoryService implements InventoryUseCases {

    private final InventoryRepository inventory;
    private final ReservationRepository reservations;
    private final MovementRepository movements;
    private final ProductUseCases products;
    private final WarehouseUseCases warehouses;
    private final TransactionRunner transactions;
    private final AccessControl accessControl;
    private final Clock clock;

    public InventoryService(InventoryRepository inventory, ReservationRepository reservations,
                            MovementRepository movements, ProductUseCases products,
                            WarehouseUseCases warehouses, TransactionRunner transactions,
                            AccessControl accessControl, Clock clock) {
        this.inventory = inventory;
        this.reservations = reservations;
        this.movements = movements;
        this.products = products;
        this.warehouses = warehouses;
        this.transactions = transactions;
        this.accessControl = accessControl;
        this.clock = clock;
    }

    @Override
    public MovementView receive(StockCommand command) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR);
        validateContext(command.warehouseId(), command.productId());
        return transactions.required(() -> movements.findByExternalReference(command.externalReference())
                .map(existing -> view(requireSameMovement(existing, command, MovementType.RECEIPT)))
                .orElseGet(() -> {
                    var balance = inventory.lockOrCreate(command.warehouseId(), command.productId());
                    balance.receive(command.quantity());
                    inventory.save(balance);
                    return view(movements.save(movement(command, MovementType.RECEIPT, null)));
                }));
    }

    @Override
    public MovementView issue(StockCommand command) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR);
        validateContext(command.warehouseId(), command.productId());
        return transactions.required(() -> movements.findByExternalReference(command.externalReference())
                .map(existing -> view(requireSameMovement(existing, command, MovementType.MANUAL_ISSUE)))
                .orElseGet(() -> {
                    var balance = inventory.lockOrCreate(command.warehouseId(), command.productId());
                    balance.issueAvailable(command.quantity());
                    inventory.save(balance);
                    return view(movements.save(movement(command, MovementType.MANUAL_ISSUE, null)));
                }));
    }

    @Override
    public ReservationView reserve(ReserveCommand command) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR, Role.ORDER_OPERATOR);
        validateContext(command.warehouseId(), command.productId());
        return transactions.required(() -> reservations.findByExternalReference(command.externalReference())
                .map(existing -> view(requireSameReservation(existing, command)))
                .orElseGet(() -> {
                    var balance = inventory.lockOrCreate(command.warehouseId(), command.productId());
                    balance.reserve(command.quantity());
                    var now = clock.instant();
                    var reservation = Reservation.create(UUID.randomUUID(), command.warehouseId(), command.productId(),
                            command.quantity(), command.externalReference(), now);
                    inventory.save(balance);
                    reservations.save(reservation);
                    movements.save(new StockMovement(UUID.randomUUID(), command.warehouseId(), command.productId(),
                            reservation.id(), MovementType.RESERVATION, command.quantity(),
                            "reserve:" + command.externalReference(), now));
                    return view(reservation);
                }));
    }

    @Override
    public ReservationView release(UUID reservationId, String externalReference) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR, Role.ORDER_OPERATOR);
        return changeReservation(reservationId, externalReference, false);
    }

    @Override
    public ReservationView consume(UUID reservationId, String externalReference) {
        accessControl.requireAny(Role.ADMIN, Role.ORDER_OPERATOR);
        return changeReservation(reservationId, externalReference, true);
    }

    private ReservationView changeReservation(UUID reservationId, String reference, boolean consume) {
        return transactions.required(() -> {
            var movementReference = (consume ? "consume:" : "release:") + reference;
            var reservation = reservations.findByIdForUpdate(reservationId).orElseThrow(() ->
                    new NotFoundException("reservation.not_found", "Reservation not found"));
            var existingMovement = movements.findByExternalReference(movementReference);
            if (existingMovement.isPresent()) {
                if (!reservation.id().equals(existingMovement.get().reservationId())) {
                    throw new ConflictException("idempotency.reference_reused",
                            "External reference was already used by another operation");
                }
                return view(reservation);
            }
            var balance = inventory.lockOrCreate(reservation.warehouseId(), reservation.productId());
            if (consume) {
                balance.consumeReserved(reservation.quantity());
                reservation.consume(clock.instant());
            } else {
                balance.release(reservation.quantity());
                reservation.release(clock.instant());
            }
            inventory.save(balance);
            reservations.save(reservation);
            movements.save(new StockMovement(UUID.randomUUID(), reservation.warehouseId(), reservation.productId(),
                    reservation.id(), consume ? MovementType.ORDER_ISSUE : MovementType.RELEASE,
                    reservation.quantity(), movementReference, clock.instant()));
            return view(reservation);
        });
    }

    @Override
    public TransferView transfer(TransferCommand command) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR);
        if (command.sourceWarehouseId().equals(command.destinationWarehouseId())) {
            throw new ConflictException("inventory.same_warehouse", "Source and destination warehouses must differ");
        }
        validateContext(command.sourceWarehouseId(), command.productId());
        validateContext(command.destinationWarehouseId(), command.productId());
        return transactions.required(() -> {
            var existingOut = movements.findByExternalReference("transfer-out:" + command.externalReference());
            if (existingOut.isPresent()) {
                var existingIn = movements.findByExternalReference("transfer-in:" + command.externalReference())
                        .orElseThrow(() -> new IllegalStateException("Incomplete idempotent transfer"));
                if (!existingOut.get().warehouseId().equals(command.sourceWarehouseId())
                        || !existingIn.warehouseId().equals(command.destinationWarehouseId())
                        || !existingOut.get().productId().equals(command.productId())
                        || existingOut.get().quantity() != command.quantity()) {
                    throw new ConflictException("idempotency.reference_reused",
                            "External reference was reused with different transfer data");
                }
                return new TransferView(view(existingOut.get()), view(existingIn));
            }

            StockBalance first;
            StockBalance second;
            if (command.sourceWarehouseId().compareTo(command.destinationWarehouseId()) < 0) {
                first = inventory.lockOrCreate(command.sourceWarehouseId(), command.productId());
                second = inventory.lockOrCreate(command.destinationWarehouseId(), command.productId());
            } else {
                first = inventory.lockOrCreate(command.destinationWarehouseId(), command.productId());
                second = inventory.lockOrCreate(command.sourceWarehouseId(), command.productId());
            }
            var source = first.warehouseId().equals(command.sourceWarehouseId()) ? first : second;
            var destination = source == first ? second : first;
            source.issueAvailable(command.quantity());
            destination.receive(command.quantity());
            inventory.save(source);
            inventory.save(destination);
            var now = clock.instant();
            var outgoing = movements.save(new StockMovement(UUID.randomUUID(), source.warehouseId(), command.productId(),
                    null, MovementType.TRANSFER_OUT, command.quantity(),
                    "transfer-out:" + command.externalReference(), now));
            var incoming = movements.save(new StockMovement(UUID.randomUUID(), destination.warehouseId(), command.productId(),
                    null, MovementType.TRANSFER_IN, command.quantity(),
                    "transfer-in:" + command.externalReference(), now));
            return new TransferView(view(outgoing), view(incoming));
        });
    }

    @Override
    public BalanceView balance(UUID warehouseId, UUID productId) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR, Role.ORDER_OPERATOR, Role.VIEWER);
        return inventory.find(warehouseId, productId).map(InventoryService::view).orElse(new BalanceView(
                warehouseId, productId, 0, 0, 0));
    }

    @Override
    public MovementPageView movements(UUID warehouseId, UUID productId, int page, int size) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR, Role.ORDER_OPERATOR, Role.VIEWER);
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
        var result = movements.find(warehouseId, productId, page, size);
        return new MovementPageView(result.content().stream().map(InventoryService::view).toList(),
                page, size, result.totalElements(), result.totalPages());
    }

    private void validateContext(UUID warehouseId, UUID productId) {
        if (!warehouses.existsActive(warehouseId)) {
            throw new NotFoundException("warehouse.not_found_or_inactive", "Active warehouse not found");
        }
        if (!products.existsActive(productId)) {
            throw new NotFoundException("product.not_found_or_inactive", "Active product not found");
        }
    }

    private StockMovement movement(StockCommand command, MovementType type, UUID reservationId) {
        return new StockMovement(UUID.randomUUID(), command.warehouseId(), command.productId(), reservationId,
                type, command.quantity(), command.externalReference(), clock.instant());
    }

    private static StockMovement requireSameMovement(
            StockMovement existing, StockCommand command, MovementType expectedType) {
        if (existing.type() != expectedType
                || !existing.warehouseId().equals(command.warehouseId())
                || !existing.productId().equals(command.productId())
                || existing.quantity() != command.quantity()) {
            throw new ConflictException("idempotency.reference_reused",
                    "External reference was reused with different operation data");
        }
        return existing;
    }

    private static Reservation requireSameReservation(Reservation existing, ReserveCommand command) {
        if (!existing.warehouseId().equals(command.warehouseId())
                || !existing.productId().equals(command.productId())
                || existing.quantity() != command.quantity()) {
            throw new ConflictException("idempotency.reference_reused",
                    "External reference was reused with different reservation data");
        }
        return existing;
    }

    private static BalanceView view(StockBalance balance) {
        return new BalanceView(balance.warehouseId(), balance.productId(), balance.onHand(),
                balance.reserved(), balance.available());
    }

    private static ReservationView view(Reservation reservation) {
        return new ReservationView(reservation.id(), reservation.warehouseId(), reservation.productId(),
                reservation.quantity(), reservation.externalReference(), reservation.status(), reservation.createdAt());
    }

    private static MovementView view(StockMovement movement) {
        return new MovementView(movement.id(), movement.warehouseId(), movement.productId(), movement.reservationId(),
                movement.type(), movement.quantity(), movement.externalReference(), movement.occurredAt());
    }
}
