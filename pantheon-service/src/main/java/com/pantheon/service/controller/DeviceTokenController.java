package com.pantheon.service.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pantheon.service.dto.DeviceTokenRegistrationRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.DeviceTokenService;
import com.pantheon.service.service.DeviceTokenService.DeviceTokenRegistrationData;

/** Lets the mobile app register/unregister its own push token, scoped to the caller. See push-notifications capability. */
@RestController
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping("/api/me/device-tokens")
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal AppUser user, @Valid @RequestBody DeviceTokenRegistrationRequest request) {
        deviceTokenService.register(user.getId(), new DeviceTokenRegistrationData(request.token(), request.platform()));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/api/me/device-tokens/{token}")
    public ResponseEntity<Void> unregister(@AuthenticationPrincipal AppUser user, @PathVariable String token) {
        deviceTokenService.unregister(user.getId(), token);
        return ResponseEntity.noContent().build();
    }
}
