package com.example.wms.shared.infrastructure;

import com.example.wms.inventory.application.InventoryRepository;
import com.example.wms.inventory.application.InventoryService;
import com.example.wms.inventory.application.InventoryUseCases;
import com.example.wms.inventory.application.MovementRepository;
import com.example.wms.inventory.application.ReservationRepository;
import com.example.wms.order.application.OrderRepository;
import com.example.wms.order.application.OrderService;
import com.example.wms.order.application.OrderUseCases;
import com.example.wms.product.application.ProductRepository;
import com.example.wms.product.application.ProductService;
import com.example.wms.product.application.ProductUseCases;
import com.example.wms.shared.application.AccessControl;
import com.example.wms.shared.application.TransactionRunner;
import com.example.wms.warehouse.application.WarehouseRepository;
import com.example.wms.warehouse.application.WarehouseService;
import com.example.wms.warehouse.application.WarehouseUseCases;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ProductUseCases productUseCases(ProductRepository repository, TransactionRunner transactions,
                                    AccessControl accessControl) {
        return new ProductService(repository, transactions, accessControl);
    }

    @Bean
    WarehouseUseCases warehouseUseCases(WarehouseRepository repository, TransactionRunner transactions,
                                        AccessControl accessControl) {
        return new WarehouseService(repository, transactions, accessControl);
    }

    @Bean
    InventoryUseCases inventoryUseCases(InventoryRepository inventory, ReservationRepository reservations,
                                        MovementRepository movements, ProductUseCases products,
                                        WarehouseUseCases warehouses, TransactionRunner transactions,
                                        AccessControl accessControl, Clock clock) {
        return new InventoryService(inventory, reservations, movements, products, warehouses,
                transactions, accessControl, clock);
    }

    @Bean
    OrderUseCases orderUseCases(OrderRepository orders, InventoryUseCases inventory,
                                WarehouseUseCases warehouses, TransactionRunner transactions,
                                AccessControl accessControl, Clock clock) {
        return new OrderService(orders, inventory, warehouses, transactions, accessControl, clock);
    }
}
