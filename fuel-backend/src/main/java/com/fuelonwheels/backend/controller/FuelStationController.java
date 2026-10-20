package com.fuelonwheels.backend.controller;

import com.fuelonwheels.backend.dto.ApiResponse;
import com.fuelonwheels.backend.dto.FuelStationDto;
import com.fuelonwheels.backend.service.FuelStationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
@Tag(name = "Fuel Stations", description = "Endpoints for finding and querying fuel stations")
public class FuelStationController {

    private final FuelStationService fuelStationService;

    public FuelStationController(FuelStationService fuelStationService) {
        this.fuelStationService = fuelStationService;
    }

    @GetMapping
    @Operation(summary = "Get all fuel stations", description = "Retrieves a list of all active fuel stations")
    public ResponseEntity<ApiResponse<List<FuelStationDto>>> getAllStations() {
        List<FuelStationDto> stations = fuelStationService.getAllStations();
        return ResponseEntity.ok(ApiResponse.ok(stations));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get station by ID", description = "Retrieves details of a specific fuel station")
    public ResponseEntity<ApiResponse<FuelStationDto>> getStationById(@PathVariable Long id) {
        FuelStationDto station = fuelStationService.getStationById(id);
        return ResponseEntity.ok(ApiResponse.ok(station));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Find nearby fuel stations", description = "Retrieves fuel stations sorted by proximity using Haversine distance")
    public ResponseEntity<ApiResponse<List<FuelStationDto>>> getNearbyStations(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "15.0") double radiusKm) {
        List<FuelStationDto> stations = fuelStationService.getNearbyStations(lat, lng, radiusKm);
        return ResponseEntity.ok(ApiResponse.ok(stations));
    }

    @GetMapping("/search")
    @Operation(summary = "Search fuel stations", description = "Search fuel stations by name or address")
    public ResponseEntity<ApiResponse<List<FuelStationDto>>> searchStations(@RequestParam String q) {
        List<FuelStationDto> stations = fuelStationService.searchStations(q);
        return ResponseEntity.ok(ApiResponse.ok(stations));
    }
}
