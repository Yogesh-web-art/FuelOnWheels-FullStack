package com.fuelonwheels.backend.controller;

import com.fuelonwheels.backend.dto.ApiResponse;
import com.fuelonwheels.backend.dto.TrackingDto;
import com.fuelonwheels.backend.dto.TrackingUpdateRequest;
import com.fuelonwheels.backend.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracking")
@Tag(name = "Delivery Tracking", description = "Endpoints for live driver tracking and delivery simulation")
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @GetMapping("/{orderCode}")
    @Operation(summary = "Get live delivery tracking", description = "Retrieves live driver coordinates, ETA, and progress status for an order")
    public ResponseEntity<ApiResponse<TrackingDto>> getTracking(@PathVariable String orderCode) {
        TrackingDto tracking = trackingService.getTracking(orderCode);
        return ResponseEntity.ok(ApiResponse.ok(tracking));
    }

    @PutMapping("/{orderCode}")
    @Operation(summary = "Update driver coordinates & ETA", description = "Endpoint for delivery driver mobile client to update live GPS coordinates")
    public ResponseEntity<ApiResponse<TrackingDto>> updateTracking(
            @PathVariable String orderCode,
            @RequestBody TrackingUpdateRequest request) {
        TrackingDto tracking = trackingService.updateTracking(orderCode, request);
        return ResponseEntity.ok(ApiResponse.ok("Tracking updated", tracking));
    }

    @PostMapping("/{orderCode}/simulate-step")
    @Operation(summary = "Simulate delivery progression step", description = "Advances the driver 35% closer towards customer location and increments status")
    public ResponseEntity<ApiResponse<TrackingDto>> simulateStep(@PathVariable String orderCode) {
        TrackingDto tracking = trackingService.simulateNextStep(orderCode);
        return ResponseEntity.ok(ApiResponse.ok("Simulated step applied", tracking));
    }
}
