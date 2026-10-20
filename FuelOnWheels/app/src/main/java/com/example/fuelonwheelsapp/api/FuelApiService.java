package com.example.fuelonwheelsapp.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;
import com.example.fuelonwheelsapp.api.ApiModels.*;

public interface FuelApiService {

    // Authentication Endpoints
    @POST("api/v1/auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("api/v1/auth/register")
    Call<ApiResponse<AuthResponse>> register(@Body RegisterRequest request);

    // Fuel Station Endpoints
    @GET("api/v1/stations")
    Call<ApiResponse<List<StationModel>>> getAllStations();

    @GET("api/v1/stations/nearby")
    Call<ApiResponse<List<StationModel>>> getNearbyStations(
            @Query("lat") double lat,
            @Query("lng") double lng,
            @Query("radiusKm") double radiusKm
    );

    @GET("api/v1/stations/{id}")
    Call<ApiResponse<StationModel>> getStationById(@Path("id") long id);

    // Order Endpoints
    @POST("api/v1/orders")
    Call<ApiResponse<BackendOrderResponse>> placeOrder(
            @Header("Authorization") String authHeader,
            @Body BackendOrderRequest request
    );

    @GET("api/v1/orders/{orderCode}")
    Call<ApiResponse<BackendOrderResponse>> getOrderByCode(@Path("orderCode") String orderCode);

    @GET("api/v1/orders/user/{userId}")
    Call<ApiResponse<List<BackendOrderResponse>>> getUserOrders(@Path("userId") long userId);

    // Real-Time Delivery Tracking Endpoints
    @GET("api/v1/tracking/{orderCode}")
    Call<ApiResponse<BackendTrackingResponse>> getTracking(@Path("orderCode") String orderCode);

    @POST("api/v1/tracking/{orderCode}/simulate-step")
    Call<ApiResponse<BackendTrackingResponse>> simulateTrackingStep(@Path("orderCode") String orderCode);
}
