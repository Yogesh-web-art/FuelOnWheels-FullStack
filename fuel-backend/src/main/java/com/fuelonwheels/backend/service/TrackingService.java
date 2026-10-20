package com.fuelonwheels.backend.service;

import com.fuelonwheels.backend.dto.TrackingDto;
import com.fuelonwheels.backend.dto.TrackingUpdateRequest;
import com.fuelonwheels.backend.entity.DeliveryTracking;
import com.fuelonwheels.backend.entity.Order;
import com.fuelonwheels.backend.entity.OrderStatus;
import com.fuelonwheels.backend.exception.ResourceNotFoundException;
import com.fuelonwheels.backend.repository.DeliveryTrackingRepository;
import com.fuelonwheels.backend.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TrackingService {

    private final DeliveryTrackingRepository trackingRepository;
    private final OrderRepository orderRepository;

    public TrackingService(DeliveryTrackingRepository trackingRepository,
                           OrderRepository orderRepository) {
        this.trackingRepository = trackingRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public TrackingDto getTracking(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with code: " + orderCode));

        DeliveryTracking tracking = trackingRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking details not found for order: " + orderCode));

        return mapToDto(order, tracking);
    }

    @Transactional
    public TrackingDto updateTracking(String orderCode, TrackingUpdateRequest request) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with code: " + orderCode));

        DeliveryTracking tracking = trackingRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking details not found for order: " + orderCode));

        if (request.getLatitude() != null) {
            tracking.setCurrentLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            tracking.setCurrentLongitude(request.getLongitude());
        }
        if (request.getEtaMinutes() != null) {
            tracking.setEtaMinutes(request.getEtaMinutes());
        }
        if (request.getStatusMessage() != null) {
            tracking.setStatusMessage(request.getStatusMessage());
        }
        if (request.getOrderStatus() != null) {
            order.setOrderStatus(request.getOrderStatus());
            orderRepository.save(order);
        }

        tracking.setUpdatedAt(LocalDateTime.now());
        DeliveryTracking updated = trackingRepository.save(tracking);

        return mapToDto(order, updated);
    }

    @Transactional
    public TrackingDto simulateNextStep(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with code: " + orderCode));

        DeliveryTracking tracking = trackingRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking details not found for order: " + orderCode));

        // Advance lifecycle state
        switch (order.getOrderStatus()) {
            case PLACED:
                order.setOrderStatus(OrderStatus.CONFIRMED);
                tracking.setStatusMessage("Order confirmed by fuel station");
                tracking.setEtaMinutes(12);
                break;
            case CONFIRMED:
                order.setOrderStatus(OrderStatus.DISPATCHED);
                tracking.setStatusMessage("Fuel tanker dispatched from station");
                tracking.setEtaMinutes(8);
                break;
            case DISPATCHED:
                order.setOrderStatus(OrderStatus.OUT_FOR_DELIVERY);
                tracking.setStatusMessage("Driver is arriving at your location");
                tracking.setEtaMinutes(3);
                break;
            case OUT_FOR_DELIVERY:
                order.setOrderStatus(OrderStatus.DELIVERED);
                tracking.setStatusMessage("Fuel successfully delivered to your vehicle!");
                tracking.setEtaMinutes(0);
                if (order.getDeliveryLatitude() != null && order.getDeliveryLongitude() != null) {
                    tracking.setCurrentLatitude(order.getDeliveryLatitude());
                    tracking.setCurrentLongitude(order.getDeliveryLongitude());
                }
                break;
            case DELIVERED:
            case CANCELLED:
                break;
        }

        // Interpolate position 30% closer towards user destination if coordinates are present
        if (order.getOrderStatus() != OrderStatus.DELIVERED
                && order.getDeliveryLatitude() != null
                && order.getDeliveryLongitude() != null) {
            double currentLat = tracking.getCurrentLatitude();
            double currentLng = tracking.getCurrentLongitude();
            double targetLat = order.getDeliveryLatitude();
            double targetLng = order.getDeliveryLongitude();

            tracking.setCurrentLatitude(currentLat + (targetLat - currentLat) * 0.35);
            tracking.setCurrentLongitude(currentLng + (targetLng - currentLng) * 0.35);
        }

        tracking.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        DeliveryTracking updated = trackingRepository.save(tracking);

        return mapToDto(order, updated);
    }

    private TrackingDto mapToDto(Order order, DeliveryTracking tracking) {
        return new TrackingDto(
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
    }
}
