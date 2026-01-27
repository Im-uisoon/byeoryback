package com.project.byeoryback.domain.pin.service;

import com.project.byeoryback.domain.pin.dto.PinRegisterRequest;
import com.project.byeoryback.domain.pin.dto.PinVerifyRequest;
import com.project.byeoryback.domain.pin.entity.Pin;
import com.project.byeoryback.domain.pin.repository.PinRepository;
import com.project.byeoryback.domain.user.entity.User;
import com.project.byeoryback.domain.user.repository.UserRepository;
import com.project.byeoryback.domain.auth.service.MailService;
import com.project.byeoryback.domain.pin.dto.PinStatusResponse;
import com.project.byeoryback.domain.pin.dto.PinUnlockRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PinService {

    private final PinRepository pinRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Transactional
    public void registerPin(Long userId, PinRegisterRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String encodedPin = passwordEncoder.encode(request.getPin());

        Pin pin = pinRepository.findByUser(user)
                .orElse(Pin.builder().user(user).build());

        pin.updatePinHash(encodedPin);

        pinRepository.save(pin);
    }

    @Transactional
    public boolean verifyPin(Long userId, PinVerifyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Pin pin = pinRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("PIN not set for this user"));

        if (pin.isLocked()) {
            throw new IllegalArgumentException("PIN blocked due to too many failed attempts");
        }

        if (passwordEncoder.matches(request.getPin(), pin.getPinHash())) {
            pin.resetFailureCount();
            return true;
        } else {
            pin.incrementFailureCount();
            return false;
        }
    }

    @Transactional(readOnly = true)
    public PinStatusResponse getPinStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return pinRepository.findByUser(user)
                .map(pin -> PinStatusResponse.builder()
                        .isRegistered(true)
                        .failureCount(pin.getFailureCount())
                        .isLocked(pin.isLocked())
                        .build())
                .orElse(PinStatusResponse.builder()
                        .isRegistered(false)
                        .failureCount(0)
                        .isLocked(false)
                        .build());
    }

    @Transactional
    public void requestUnlock(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Pin pin = pinRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("PIN not set for this user"));

        if (!pin.isLocked()) {
            throw new IllegalArgumentException("PIN is not locked");
        }

        try {
            mailService.sendSimpleMessage(user.getEmail());
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email verification code", e);
        }
    }

    @Transactional
    public void unlockPin(Long userId, PinUnlockRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Pin pin = pinRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("PIN not set for this user"));

        if (!pin.isLocked()) {
            throw new IllegalArgumentException("PIN is not locked");
        }

        boolean isVerified = mailService.verifyEmailCode(user.getEmail(), request.getCode());
        if (!isVerified) {
            throw new IllegalArgumentException("Invalid verification code");
        }

        user.removePin(); // Explicitly break relationship
        pinRepository.delete(pin);
    }

    @Transactional(readOnly = true)
    public boolean checkPinSet(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return pinRepository.existsByUser(user);
    }

    @Transactional
    public void deletePin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Pin pin = pinRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("PIN not set"));
        pinRepository.delete(pin);
    }
}
