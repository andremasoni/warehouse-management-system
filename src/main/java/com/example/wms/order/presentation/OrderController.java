package com.example.wms.order.presentation;

import com.example.wms.order.application.OrderUseCases;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderUseCases useCases;

    public OrderController(OrderUseCases useCases) {
        this.useCases = useCases;
    }

    @PostMapping
    public ResponseEntity<OrderUseCases.OrderView> create(@Valid @RequestBody CreateOrderRequest request) {
        var created = useCases.create(new OrderUseCases.CreateOrderCommand(request.warehouseId(),
                request.externalReference(), request.items().stream()
                .map(item -> new OrderUseCases.ItemCommand(item.productId(), item.quantity())).toList()));
        return ResponseEntity.created(URI.create("/api/orders/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    public OrderUseCases.OrderView get(@PathVariable UUID id) { return useCases.get(id); }

    @PostMapping("/{id}/confirmation")
    public OrderUseCases.OrderView confirm(@PathVariable UUID id) { return useCases.confirm(id); }

    @PostMapping("/{id}/cancellation")
    public OrderUseCases.OrderView cancel(@PathVariable UUID id) { return useCases.cancel(id); }

    public record CreateOrderRequest(@NotNull UUID warehouseId,
                                     @NotBlank @Size(max = 100) String externalReference,
                                     @NotEmpty @Size(max = 100) List<@Valid ItemRequest> items) {}
    public record ItemRequest(@NotNull UUID productId, @Positive long quantity) {}
}
