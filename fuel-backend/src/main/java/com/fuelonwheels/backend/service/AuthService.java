package com.fuelonwheels.backend.service;

import com.fuelonwheels.backend.dto.AuthRequest;
import com.fuelonwheels.backend.dto.AuthResponse;
import com.fuelonwheels.backend.dto.RegisterRequest;
import com.fuelonwheels.backend.entity.Role;
import com.fuelonwheels.backend.entity.User;
import com.fuelonwheels.backend.exception.BadRequestException;
import com.fuelonwheels.backend.repository.UserRepository;
import com.fuelonwheels.backend.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number is already registered: " + request.getPhone());
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPhone(request.getPhone().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAddress(request.getAddress());
        user.setRole(Role.ROLE_CUSTOMER);

        // Generate 6-digit verification OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setVerificationOtp(otp);

        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole().name());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getPhone(),
                savedUser.getRole().name(),
                savedUser.getVerificationOtp()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String username = request.getUsername().trim();

        User user = userRepository.findByEmail(username.toLowerCase())
                .or(() -> userRepository.findByPhone(username))
                .orElseThrow(() -> new BadCredentialsException("Invalid email/phone or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email/phone or password");
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getVerificationOtp()
        );
    }
}
