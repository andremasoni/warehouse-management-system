package com.example.wms.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wms.inventory.application.InventoryUseCases;
import com.example.wms.product.application.ProductUseCases;
import com.example.wms.shared.application.AccessControl;
import com.example.wms.warehouse.application.WarehouseUseCases;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class InventoryConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @MockitoBean
    AccessControl accessControl;

    @Autowired ProductUseCases products;
    @Autowired WarehouseUseCases warehouses;
    @Autowired InventoryUseCases inventory;

    @Test
    void concurrentReservationsCannotOversellLastUnit() throws Exception {
        var product = products.create(new ProductUseCases.CreateProductCommand("CONCURRENT-1", "Concurrent item"));
        var warehouse = warehouses.create(new WarehouseUseCases.CreateWarehouseCommand("MAIN", "Main warehouse"));
        inventory.receive(new InventoryUseCases.StockCommand(
                warehouse.id(), product.id(), 1, "receipt-concurrent-test"));

        Callable<Boolean> first = () -> reserve(warehouse.id(), product.id(), "concurrent-a");
        Callable<Boolean> second = () -> reserve(warehouse.id(), product.id(), "concurrent-b");

        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(java.util.List.of(first, second)).stream()
                    .map(future -> {
                        try { return future.get(); } catch (Exception ignored) { return false; }
                    }).toList();
            assertThat(results).containsExactlyInAnyOrder(true, false);
        }
        assertThat(inventory.balance(warehouse.id(), product.id()).reserved()).isEqualTo(1);
    }

    private boolean reserve(UUID warehouseId, UUID productId, String reference) {
        try {
            inventory.reserve(new InventoryUseCases.ReserveCommand(warehouseId, productId, 1, reference));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
