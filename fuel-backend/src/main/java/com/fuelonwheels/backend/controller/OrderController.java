package com.fuelonwheels.backend.controller;

import com.fuelonwheels.backend.dto.ApiResponse;
import com.fuelonwheels.backend.dto.OrderRequest;
import com.fuelonwheels.backend.dto.OrderResponse;
import com.fuelonwheels.backend.entity.OrderStatus;
import com.fuelonwheels.backend.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Endpoints for fuel order lifecycle management")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place a new fuel order", description = "Creates a fuel delivery order and automatically dispatches a delivery tracking entity")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", response));
    }

    @GetMapping("/{orderCode}")
    @Operation(summary = "Get order by code", description = "Retrieves order and live tracking details by order code (e.g. FOW-123456)")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByCode(@PathVariable String orderCode) {
        OrderResponse response = orderService.getOrderByCode(orderCode);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user order history", description = "Retrieves all past and active orders placed by a specific user")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(@PathVariable Long userId) {
        List<OrderResponse> orders = orderService.getUserOrders(userId);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @PatchMapping("/{orderCode}/status")
    @Operation(summary = "Update order status", description = "Updates order status (PLACED, CONFIRMED, DISPATCHED, OUT_FOR_DELIVERY, DELIVERED, CANCELLED)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable String orderCode,
            @RequestParam OrderStatus status) {
        OrderResponse response = orderService.updateOrderStatus(orderCode, status);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated", response));
    }
}
