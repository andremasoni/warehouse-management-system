package com.example.wms.order.application;

import com.example.wms.inventory.application.InventoryUseCases;
import com.example.wms.order.domain.OrderItem;
import com.example.wms.order.domain.OrderStatus;
import com.example.wms.order.domain.SalesOrder;
import com.example.wms.shared.application.AccessControl;
import com.example.wms.shared.application.Role;
import com.example.wms.shared.application.TransactionRunner;
import com.example.wms.shared.domain.NotFoundException;
import com.example.wms.warehouse.application.WarehouseUseCases;
import java.time.Clock;
import java.util.UUID;

public final class OrderService implements OrderUseCases {
    private final OrderRepository orders;
    private final InventoryUseCases inventory;
    private final WarehouseUseCases warehouses;
    private final TransactionRunner transactions;
    private final AccessControl accessControl;
    private final Clock clock;

    public OrderService(OrderRepository orders, InventoryUseCases inventory, WarehouseUseCases warehouses,
                        TransactionRunner transactions, AccessControl accessControl, Clock clock) {
        this.orders = orders;
        this.inventory = inventory;
        this.warehouses = warehouses;
        this.transactions = transactions;
        this.accessControl = accessControl;
        this.clock = clock;
    }

    @Override
    public OrderView create(CreateOrderCommand command) {
        accessControl.requireAny(Role.ADMIN, Role.ORDER_OPERATOR);
        if (!warehouses.existsActive(command.warehouseId())) {
            throw new NotFoundException("warehouse.not_found_or_inactive", "Active warehouse not found");
        }
        return transactions.required(() -> orders.findByExternalReference(command.externalReference())
                .map(existing -> view(requireSameOrder(existing, command)))
                .orElseGet(() -> {
                    var items = command.items().stream()
                            .map(item -> OrderItem.create(item.productId(), item.quantity())).toList();
                    var now = clock.instant();
                    var order = SalesOrder.create(UUID.randomUUID(), command.warehouseId(),
                            command.externalReference(), items, now);
                    for (var item : order.items()) {
                        var reservation = inventory.reserve(new InventoryUseCases.ReserveCommand(
                                order.warehouseId(), item.productId(), item.quantity(),
                                "order:" + order.id() + ":item:" + item.id()));
                        item.attachReservation(reservation.id());
                    }
                    order.markReserved(clock.instant());
                    return view(orders.save(order));
                }));
    }

    @Override
    public OrderView get(UUID id) {
        accessControl.requireAny(Role.ADMIN, Role.ORDER_OPERATOR, Role.VIEWER);
        return orders.findById(id).map(OrderService::view).orElseThrow(() ->
                new NotFoundException("order.not_found", "Order not found"));
    }

    @Override
    public OrderView confirm(UUID id) {
        accessControl.requireAny(Role.ADMIN, Role.ORDER_OPERATOR);
        return transactions.required(() -> {
            var order = findForUpdate(id);
            if (order.status() == OrderStatus.CONFIRMED) return view(order);
            for (var item : order.items()) {
                inventory.consume(item.reservationId(), "order:" + order.id() + ":confirm:" + item.id());
            }
            order.confirm(clock.instant());
            return view(orders.save(order));
        });
    }

    @Override
    public OrderView cancel(UUID id) {
        accessControl.requireAny(Role.ADMIN, Role.ORDER_OPERATOR);
        return transactions.required(() -> {
            var order = findForUpdate(id);
            if (order.status() == OrderStatus.CANCELLED) return view(order);
            for (var item : order.items()) {
                inventory.release(item.reservationId(), "order:" + order.id() + ":cancel:" + item.id());
            }
            order.cancel(clock.instant());
            return view(orders.save(order));
        });
    }

    private SalesOrder findForUpdate(UUID id) {
        return orders.findByIdForUpdate(id).orElseThrow(() ->
                new NotFoundException("order.not_found", "Order not found"));
    }

    private static SalesOrder requireSameOrder(SalesOrder existing, CreateOrderCommand command) {
        var sameItems = existing.items().size() == command.items().size()
                && existing.items().stream().allMatch(item -> command.items().stream().anyMatch(candidate ->
                candidate.productId().equals(item.productId()) && candidate.quantity() == item.quantity()));
        if (!existing.warehouseId().equals(command.warehouseId()) || !sameItems) {
            throw new com.example.wms.shared.domain.ConflictException("idempotency.reference_reused",
                    "External reference was reused with different order data");
        }
        return existing;
    }

    private static OrderView view(SalesOrder order) {
        return new OrderView(order.id(), order.warehouseId(), order.externalReference(), order.status(),
                order.items().stream().map(item -> new ItemView(item.id(), item.productId(), item.quantity(),
                        item.reservationId())).toList(), order.createdAt(), order.updatedAt());
    }
}
