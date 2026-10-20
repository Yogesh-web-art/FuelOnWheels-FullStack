package com.fuelonwheels.backend.config;

import com.fuelonwheels.backend.entity.FuelStation;
import com.fuelonwheels.backend.entity.Role;
import com.fuelonwheels.backend.entity.User;
import com.fuelonwheels.backend.repository.FuelStationRepository;
import com.fuelonwheels.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final FuelStationRepository stationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(FuelStationRepository stationRepository,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.stationRepository = stationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed default stations if empty
        if (stationRepository.count() == 0) {
            logger.info("Seeding initial Fuel Stations into database...");

            FuelStation s1 = new FuelStation(
                    "IndianOil Fuel Station - Indiranagar",
                    "IndianOil",
                    "100 Feet Rd, HAL 2nd Stage, Indiranagar, Bengaluru, Karnataka 560038",
                    12.9784,
                    77.6408,
                    BigDecimal.valueOf(102.86),
                    BigDecimal.valueOf(88.94),
                    4.6,
                    "+91 80 2525 1111"
            );

            FuelStation s2 = new FuelStation(
                    "Bharat Petroleum (BPCL) - Koramangala",
                    "Bharat Petroleum",
                    "80 Feet Rd, 4th Block, Koramangala, Bengaluru, Karnataka 560034",
                    12.9352,
                    77.6245,
                    BigDecimal.valueOf(102.80),
                    BigDecimal.valueOf(88.90),
                    4.5,
                    "+91 80 2553 2222"
            );

            FuelStation s3 = new FuelStation(
                    "Hindustan Petroleum (HP) - Whitefield",
                    "HP",
                    "Whitefield Main Rd, Pattandur Agrahara, Bengaluru, Karnataka 560066",
                    12.9698,
                    77.7499,
                    BigDecimal.valueOf(103.10),
                    BigDecimal.valueOf(89.15),
                    4.4,
                    "+91 80 2845 3333"
            );

            FuelStation s4 = new FuelStation(
                    "Shell Fuel Station - Bellandur",
                    "Shell",
                    "Outer Ring Rd, Green Glen Layout, Bellandur, Bengaluru, Karnataka 560103",
                    12.9250,
                    77.6775,
                    BigDecimal.valueOf(105.50),
                    BigDecimal.valueOf(91.20),
                    4.8,
                    "+91 80 2574 4444"
            );

            FuelStation s5 = new FuelStation(
                    "IndianOil Auto Fuel - MG Road",
                    "IndianOil",
                    "Mahatma Gandhi Rd, Ashok Nagar, Bengaluru, Karnataka 560001",
                    12.9756,
                    77.6097,
                    BigDecimal.valueOf(102.86),
                    BigDecimal.valueOf(88.94),
                    4.7,
                    "+91 80 2558 5555"
            );

            stationRepository.saveAll(Arrays.asList(s1, s2, s3, s4, s5));
            logger.info("Successfully seeded 5 initial fuel stations.");
        }

        // Seed demo user accounts if empty
        if (userRepository.count() == 0) {
            logger.info("Seeding initial Demo Users into database...");

            User demoCustomer = new User(
                    "Demo Customer",
                    "user@fuelonwheels.com",
                    "+91 9999988888",
                    passwordEncoder.encode("password123"),
                    Role.ROLE_CUSTOMER
            );
            demoCustomer.setAddress("42, 5th Cross, Koramangala 3rd Block, Bengaluru");
            demoCustomer.setVerificationOtp("123456");

            User demoAdmin = new User(
                    "System Admin",
                    "admin@fuelonwheels.com",
                    "+91 8888877777",
                    passwordEncoder.encode("admin123"),
                    Role.ROLE_ADMIN
            );
            demoAdmin.setAddress("HQ Office, Indiranagar, Bengaluru");
            demoAdmin.setVerificationOtp("999999");

            userRepository.saveAll(Arrays.asList(demoCustomer, demoAdmin));
            logger.info("Successfully seeded Demo Customer (user@fuelonwheels.com) and Admin (admin@fuelonwheels.com).");
        }
    }
}
