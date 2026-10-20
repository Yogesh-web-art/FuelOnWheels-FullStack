package com.fuelonwheels.backend.dto;

import com.fuelonwheels.backend.entity.FuelType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OrderRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Fuel Station ID is required")
    private Long stationId;

    @NotNull(message = "Fuel type is required (PETROL or DIESEL)")
    private FuelType fuelType;

    @NotNull(message = "Quantity in litres is required")
    @DecimalMin(value = "1.0", message = "Minimum fuel order quantity is 1 litre")
    private Double quantityLitres;

    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    private Double deliveryLatitude;

    private Double deliveryLongitude;

    private String paymentMethod = "UPI"; // UPI, CARD, CASH

    public OrderRequest() {
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStationId() {
        return stationId;
    }

    public void setStationId(Long stationId) {
        this.stationId = stationId;
    }

    public FuelType getFuelType() {
        return fuelType;
    }

    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    public Double getQuantityLitres() {
        return quantityLitres;
    }

    public void setQuantityLitres(Double quantityLitres) {
        this.quantityLitres = quantityLitres;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public Double getDeliveryLatitude() {
        return deliveryLatitude;
    }

    public void setDeliveryLatitude(Double deliveryLatitude) {
        this.deliveryLatitude = deliveryLatitude;
    }

    public Double getDeliveryLongitude() {
        return deliveryLongitude;
    }

    public void setDeliveryLongitude(Double deliveryLongitude) {
        this.deliveryLongitude = deliveryLongitude;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
