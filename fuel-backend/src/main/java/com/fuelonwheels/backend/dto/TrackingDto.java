package com.fuelonwheels.backend.dto;

import com.fuelonwheels.backend.entity.OrderStatus;
import java.time.LocalDateTime;

public class TrackingDto {

    private String orderCode;
    private OrderStatus orderStatus;
    private String driverName;
    private String driverPhone;
    private Double currentLatitude;
    private Double currentLongitude;
    private Integer etaMinutes;
    private String statusMessage;
    private LocalDateTime updatedAt;

    public TrackingDto() {
    }

    public TrackingDto(String orderCode, OrderStatus orderStatus, String driverName, String driverPhone,
                       Double currentLatitude, Double currentLongitude, Integer etaMinutes,
                       String statusMessage, LocalDateTime updatedAt) {
        this.orderCode = orderCode;
        this.orderStatus = orderStatus;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.etaMinutes = etaMinutes;
        this.statusMessage = statusMessage;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(Double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(Double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public Integer getEtaMinutes() {
        return etaMinutes;
    }

    public void setEtaMinutes(Integer etaMinutes) {
        this.etaMinutes = etaMinutes;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
