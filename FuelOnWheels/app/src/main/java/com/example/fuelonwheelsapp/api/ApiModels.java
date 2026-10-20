package com.example.fuelonwheelsapp.api;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class ApiModels {

    // Generic API Response Wrapper
    public static class ApiResponse<T> implements Serializable {
        @SerializedName("success")
        private boolean success;

        @SerializedName("message")
        private String message;

        @SerializedName("data")
        private T data;

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
    }

    // Login Request
    public static class LoginRequest {
        @SerializedName("username")
        private String username;

        @SerializedName("password")
        private String password;

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    // Register Request
    public static class RegisterRequest {
        @SerializedName("fullName")
        private String fullName;

        @SerializedName("email")
        private String email;

        @SerializedName("phone")
        private String phone;

        @SerializedName("password")
        private String password;

        @SerializedName("address")
        private String address;

        public RegisterRequest(String fullName, String email, String phone, String password, String address) {
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.password = password;
            this.address = address;
        }
    }

    // Auth Response
    public static class AuthResponse implements Serializable {
        @SerializedName("token")
        private String token;

        @SerializedName("tokenType")
        private String tokenType;

        @SerializedName("userId")
        private Long userId;

        @SerializedName("fullName")
        private String fullName;

        @SerializedName("email")
        private String email;

        @SerializedName("phone")
        private String phone;

        @SerializedName("role")
        private String role;

        @SerializedName("verificationOtp")
        private String verificationOtp;

        public String getToken() { return token; }
        public Long getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getRole() { return role; }
        public String getVerificationOtp() { return verificationOtp; }
    }

    // Fuel Station Model
    public static class StationModel implements Serializable {
        @SerializedName("id")
        private Long id;

        @SerializedName("name")
        private String name;

        @SerializedName("brand")
        private String brand;

        @SerializedName("address")
        private String address;

        @SerializedName("latitude")
        private Double latitude;

        @SerializedName("longitude")
        private Double longitude;

        @SerializedName("petrolPrice")
        private Double petrolPrice;

        @SerializedName("dieselPrice")
        private Double dieselPrice;

        @SerializedName("rating")
        private Double rating;

        @SerializedName("openNow")
        private Boolean openNow;

        @SerializedName("contactPhone")
        private String contactPhone;

        @SerializedName("distanceKm")
        private Double distanceKm;

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getBrand() { return brand; }
        public String getAddress() { return address; }
        public Double getLatitude() { return latitude; }
        public Double getLongitude() { return longitude; }
        public Double getPetrolPrice() { return petrolPrice; }
        public Double getDieselPrice() { return dieselPrice; }
        public Double getRating() { return rating; }
        public Boolean getOpenNow() { return openNow; }
        public String getContactPhone() { return contactPhone; }
        public Double getDistanceKm() { return distanceKm; }
    }

    // Backend Order Request
    public static class BackendOrderRequest {
        @SerializedName("userId")
        private Long userId;

        @SerializedName("stationId")
        private Long stationId;

        @SerializedName("fuelType")
        private String fuelType; // "PETROL" or "DIESEL"

        @SerializedName("quantityLitres")
        private Double quantityLitres;

        @SerializedName("deliveryAddress")
        private String deliveryAddress;

        @SerializedName("deliveryLatitude")
        private Double deliveryLatitude;

        @SerializedName("deliveryLongitude")
        private Double deliveryLongitude;

        @SerializedName("paymentMethod")
        private String paymentMethod;

        public BackendOrderRequest(Long userId, Long stationId, String fuelType, Double quantityLitres,
                                   String deliveryAddress, Double deliveryLatitude, Double deliveryLongitude,
                                   String paymentMethod) {
            this.userId = userId;
            this.stationId = stationId;
            this.fuelType = fuelType;
            this.quantityLitres = quantityLitres;
            this.deliveryAddress = deliveryAddress;
            this.deliveryLatitude = deliveryLatitude;
            this.deliveryLongitude = deliveryLongitude;
            this.paymentMethod = paymentMethod;
        }
    }

    // Backend Order Response
    public static class BackendOrderResponse implements Serializable {
        @SerializedName("id")
        private Long id;

        @SerializedName("orderCode")
        private String orderCode;

        @SerializedName("userId")
        private Long userId;

        @SerializedName("userName")
        private String userName;

        @SerializedName("stationName")
        private String stationName;

        @SerializedName("fuelType")
        private String fuelType;

        @SerializedName("quantityLitres")
        private Double quantityLitres;

        @SerializedName("unitPrice")
        private Double unitPrice;

        @SerializedName("deliveryFee")
        private Double deliveryFee;

        @SerializedName("totalAmount")
        private Double totalAmount;

        @SerializedName("orderStatus")
        private String orderStatus;

        @SerializedName("deliveryAddress")
        private String deliveryAddress;

        @SerializedName("verificationOtp")
        private String verificationOtp;

        @SerializedName("tracking")
        private BackendTrackingResponse tracking;

        public Long getId() { return id; }
        public String getOrderCode() { return orderCode; }
        public Long getUserId() { return userId; }
        public String getUserName() { return userName; }
        public String getStationName() { return stationName; }
        public String getFuelType() { return fuelType; }
        public Double getQuantityLitres() { return quantityLitres; }
        public Double getUnitPrice() { return unitPrice; }
        public Double getDeliveryFee() { return deliveryFee; }
        public Double getTotalAmount() { return totalAmount; }
        public String getOrderStatus() { return orderStatus; }
        public String getDeliveryAddress() { return deliveryAddress; }
        public String getVerificationOtp() { return verificationOtp; }
        public BackendTrackingResponse getTracking() { return tracking; }
    }

    // Tracking Response
    public static class BackendTrackingResponse implements Serializable {
        @SerializedName("orderCode")
        private String orderCode;

        @SerializedName("orderStatus")
        private String orderStatus;

        @SerializedName("driverName")
        private String driverName;

        @SerializedName("driverPhone")
        private String driverPhone;

        @SerializedName("currentLatitude")
        private Double currentLatitude;

        @SerializedName("currentLongitude")
        private Double currentLongitude;

        @SerializedName("etaMinutes")
        private Integer etaMinutes;

        @SerializedName("statusMessage")
        private String statusMessage;

        public String getOrderCode() { return orderCode; }
        public String getOrderStatus() { return orderStatus; }
        public String getDriverName() { return driverName; }
        public String getDriverPhone() { return driverPhone; }
        public Double getCurrentLatitude() { return currentLatitude; }
        public Double getCurrentLongitude() { return currentLongitude; }
        public Integer getEtaMinutes() { return etaMinutes; }
        public String getStatusMessage() { return statusMessage; }
    }
}
