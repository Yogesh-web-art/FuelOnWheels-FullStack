package com.fuelonwheels.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "fuel_stations")
public class FuelStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 60)
    private String brand; // IndianOil, Bharat Petroleum, HP, Shell

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal petrolPrice;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal dieselPrice;

    private Double rating = 4.5;

    @Column(name = "is_open")
    private Boolean openNow = true;

    @Column(length = 20)
    private String contactPhone;

    public FuelStation() {
    }

    public FuelStation(String name, String brand, String address, Double latitude, Double longitude,
                       BigDecimal petrolPrice, BigDecimal dieselPrice, Double rating, String contactPhone) {
        this.name = name;
        this.brand = brand;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.petrolPrice = petrolPrice;
        this.dieselPrice = dieselPrice;
        this.rating = rating;
        this.contactPhone = contactPhone;
        this.openNow = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public BigDecimal getPetrolPrice() {
        return petrolPrice;
    }

    public void setPetrolPrice(BigDecimal petrolPrice) {
        this.petrolPrice = petrolPrice;
    }

    public BigDecimal getDieselPrice() {
        return dieselPrice;
    }

    public void setDieselPrice(BigDecimal dieselPrice) {
        this.dieselPrice = dieselPrice;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Boolean getOpenNow() {
        return openNow;
    }

    public void setOpenNow(Boolean openNow) {
        this.openNow = openNow;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }
}
