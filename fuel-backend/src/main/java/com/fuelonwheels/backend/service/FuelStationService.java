package com.fuelonwheels.backend.service;

import com.fuelonwheels.backend.dto.FuelStationDto;
import com.fuelonwheels.backend.entity.FuelStation;
import com.fuelonwheels.backend.exception.ResourceNotFoundException;
import com.fuelonwheels.backend.repository.FuelStationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FuelStationService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final FuelStationRepository fuelStationRepository;

    public FuelStationService(FuelStationRepository fuelStationRepository) {
        this.fuelStationRepository = fuelStationRepository;
    }

    @Transactional(readOnly = true)
    public List<FuelStationDto> getAllStations() {
        return fuelStationRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FuelStationDto getStationById(Long id) {
        FuelStation station = fuelStationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fuel station not found with id: " + id));
        return mapToDto(station);
    }

    @Transactional(readOnly = true)
    public List<FuelStationDto> getNearbyStations(double userLat, double userLng, double radiusKm) {
        List<FuelStation> stations = fuelStationRepository.findByOpenNowTrue();

        return stations.stream()
                .map(station -> {
                    FuelStationDto dto = mapToDto(station);
                    double dist = calculateDistanceKm(userLat, userLng, station.getLatitude(), station.getLongitude());
                    dto.setDistanceKm(BigDecimal.valueOf(dist).setScale(2, RoundingMode.HALF_UP).doubleValue());
                    return dto;
                })
                .filter(dto -> dto.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparing(FuelStationDto::getDistanceKm))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FuelStationDto> searchStations(String query) {
        return fuelStationRepository.searchStations(query).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public FuelStationDto mapToDto(FuelStation station) {
        FuelStationDto dto = new FuelStationDto();
        dto.setId(station.getId());
        dto.setName(station.getName());
        dto.setBrand(station.getBrand());
        dto.setAddress(station.getAddress());
        dto.setLatitude(station.getLatitude());
        dto.setLongitude(station.getLongitude());
        dto.setPetrolPrice(station.getPetrolPrice());
        dto.setDieselPrice(station.getDieselPrice());
        dto.setRating(station.getRating());
        dto.setOpenNow(station.getOpenNow());
        dto.setContactPhone(station.getContactPhone());
        return dto;
    }
}
