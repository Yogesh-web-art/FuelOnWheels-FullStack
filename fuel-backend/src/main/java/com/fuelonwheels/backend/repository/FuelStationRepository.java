package com.fuelonwheels.backend.repository;

import com.fuelonwheels.backend.entity.FuelStation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FuelStationRepository extends JpaRepository<FuelStation, Long> {

    List<FuelStation> findByOpenNowTrue();

    List<FuelStation> findByBrandIgnoreCase(String brand);

    @Query("SELECT s FROM FuelStation s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.address) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<FuelStation> searchStations(@Param("query") String query);
}
