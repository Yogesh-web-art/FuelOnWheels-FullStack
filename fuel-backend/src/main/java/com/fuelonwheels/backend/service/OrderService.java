package com.fuelonwheels.backend.service;

import com.fuelonwheels.backend.dto.OrderRequest;
import com.fuelonwheels.backend.dto.OrderResponse;
import com.fuelonwheels.backend.dto.TrackingDto;
import com.fuelonwheels.backend.entity.*;
import com.fuelonwheels.backend.exception.ResourceNotFoundException;
import com.fuelonwheels.backend.repository.DeliveryTrackingRepository;
import com.fuelonwheels.backend.repository.FuelStationRepository;
import com.fuelonwheels.backend.repository.OrderRepository;
import com.fuelonwheels.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final FuelStationRepository fuelStationRepository;
    private final DeliveryTrackingRepository trackingRepository;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        FuelStationRepository fuelStationRepository,
                        DeliveryTrackingRepository trackingRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.fuelStationRepository = fuelStationRepository;
        this.trackingRepository = trackingRepository;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        FuelStation station = fuelStationRepository.findById(request.getStationId())
                .orElseThrow(() -> new ResourceNotFoundException("Fuel station not found with id: " + request.getStationId()));

        BigDecimal unitPrice = request.getFuelType() == FuelType.PETROL
                ? station.getPetrolPrice()
                : station.getDieselPrice();

        BigDecimal fuelTotal = unitPrice.multiply(BigDecimal.valueOf(request.getQuantityLitres()));
        BigDecimal deliveryFee = BigDecimal.valueOf(50.00); // Standard flat delivery fee in INR
        BigDecimal totalAmount = fuelTotal.add(deliveryFee).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setOrderCode("FOW-" + (100000 + new Random().nextInt(900000)));
        order.setUser(user);
        order.setFuelStation(station);
        order.setFuelType(request.getFuelType());
        order.setQuantityLitres(request.getQuantityLitres());
        order.setUnitPrice(unitPrice);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(totalAmount);
        order.setOrderStatus(OrderStatus.PLACED);
        order.setDeliveryAddress(request.getDeliveryAddress());
        order.setDeliveryLatitude(request.getDeliveryLatitude());
        order.setDeliveryLongitude(request.getDeliveryLongitude());
        order.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "UPI");
        order.setPaymentStatus("PAID");

        // Generate 6-digit delivery verification OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        order.setVerificationOtp(otp);

        Order savedOrder = orderRepository.save(order);

        // Assign initial DeliveryTracking
        DeliveryTracking tracking = new DeliveryTracking();
        tracking.setOrder(savedOrder);
        tracking.setDriverName("Ramesh Kumar");
        tracking.setDriverPhone("+91 9876543210");
        tracking.setCurrentLatitude(station.getLatitude());
        tracking.setCurrentLongitude(station.getLongitude());
        tracking.setEtaMinutes(15);
        tracking.setStatusMessage("Driver assigned and at " + station.getName());
        trackingRepository.save(tracking);

        savedOrder.setTracking(tracking);

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with code: " + orderCode));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderCode, OrderStatus newStatus) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with code: " + orderCode));

        order.setOrderStatus(newStatus);
        Order updated = orderRepository.save(order);
        return mapToResponse(updated);
    }

    public OrderResponse mapToResponse(Order order) {
        OrderResponse res = new OrderResponse();
        res.setId(order.getId());
        res.setOrderCode(order.getOrderCode());
        res.setUserId(order.getUser().getId());
        res.setUserName(order.getUser().getFullName());
        res.setUserPhone(order.getUser().getPhone());
        res.setStationId(order.getFuelStation().getId());
        res.setStationName(order.getFuelStation().getName());
        res.setStationAddress(order.getFuelStation().getAddress());
        res.setFuelType(order.getFuelType());
        res.setQuantityLitres(order.getQuantityLitres());
        res.setUnitPrice(order.getUnitPrice());
        res.setDeliveryFee(order.getDeliveryFee());
        res.setTotalAmount(order.getTotalAmount());
        res.setOrderStatus(order.getOrderStatus());
        res.setDeliveryAddress(order.getDeliveryAddress());
        res.setDeliveryLatitude(order.getDeliveryLatitude());
        res.setDeliveryLongitude(order.getDeliveryLongitude());
        res.setPaymentMethod(order.getPaymentMethod());
        res.setPaymentStatus(order.getPaymentStatus());
        res.setVerificationOtp(order.getVerificationOtp());
        res.setCreatedAt(order.getCreatedAt());
        res.setUpdatedAt(order.getUpdatedAt());

        DeliveryTracking tracking = order.getTracking();
        if (tracking == null) {
            tracking = trackingRepository.findByOrderId(order.getId()).orElse(null);
        }

        if (tracking != null) {
            TrackingDto trackingDto = new TrackingDto(
                    order.getOrderCode(),
                    order.getOrderStatus(),
                    tracking.getDriverName(),
                    tracking.getDriverPhone(),
                    tracking.getCurrentLatitude(),
                    tracking.getCurrentLongitude(),
                    tracking.getEtaMinutes(),
                    tracking.getStatusMessage(),
                    tracking.getUpdatedAt()
            );
            res.setTracking(trackingDto);
        }

        return res;
    }
}
