package com.pantheon.service.controller;

import com.pantheon.service.dto.UpdateUserProfileRequest;
import com.pantheon.service.dto.UserProfileResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/profile")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    // Always 200: the account's email is known regardless of whether a UserProfile row exists
    // yet, and the frontend form needs it (disabled) either way — a 204 here would leave a
    // first-time user's form with nothing to show at all, not even their own email.
    @GetMapping
    public ResponseEntity<UserProfileResponse> get(@AuthenticationPrincipal AppUser user) {
        UserProfileResponse response = userProfileService.get(user.getId())
                .map(profile -> UserProfileResponse.from(profile, user.getEmail()))
                .orElseGet(() -> UserProfileResponse.empty(user.getEmail()));
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> update(
            @AuthenticationPrincipal AppUser user, @Valid @RequestBody UpdateUserProfileRequest request) {
        var profile = userProfileService.upsert(
                user.getId(), request.cnpjCpf(), request.legalName(), request.address(), request.postalCode());
        return ResponseEntity.ok(UserProfileResponse.from(profile, user.getEmail()));
    }
}
