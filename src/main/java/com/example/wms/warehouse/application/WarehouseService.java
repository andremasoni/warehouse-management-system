package com.example.wms.warehouse.application;

import com.example.wms.shared.application.AccessControl;
import com.example.wms.shared.application.Role;
import com.example.wms.shared.application.TransactionRunner;
import com.example.wms.shared.domain.ConflictException;
import com.example.wms.shared.domain.NotFoundException;
import com.example.wms.warehouse.domain.Warehouse;
import java.util.Locale;
import java.util.UUID;

public final class WarehouseService implements WarehouseUseCases {

    private final WarehouseRepository warehouses;
    private final TransactionRunner transactions;
    private final AccessControl accessControl;

    public WarehouseService(WarehouseRepository warehouses, TransactionRunner transactions, AccessControl accessControl) {
        this.warehouses = warehouses;
        this.transactions = transactions;
        this.accessControl = accessControl;
    }

    @Override
    public WarehouseView create(CreateWarehouseCommand command) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR);
        return transactions.required(() -> {
            var normalizedCode = command.code().trim().toUpperCase(Locale.ROOT);
            warehouses.findByCode(normalizedCode).ifPresent(existing -> {
                throw new ConflictException("warehouse.code_already_exists", "A warehouse with this code already exists");
            });
            return view(warehouses.save(Warehouse.create(UUID.randomUUID(), normalizedCode, command.name())));
        });
    }

    @Override
    public WarehouseView get(UUID id) {
        accessControl.requireAny(Role.ADMIN, Role.STOCK_OPERATOR, Role.ORDER_OPERATOR, Role.VIEWER);
        return warehouses.findById(id).map(WarehouseService::view).orElseThrow(() ->
                new NotFoundException("warehouse.not_found", "Warehouse not found"));
    }

    @Override
    public boolean existsActive(UUID id) {
        return warehouses.findById(id).filter(Warehouse::active).isPresent();
    }

    private static WarehouseView view(Warehouse warehouse) {
        return new WarehouseView(warehouse.id(), warehouse.code(), warehouse.name(), warehouse.active());
    }
}
