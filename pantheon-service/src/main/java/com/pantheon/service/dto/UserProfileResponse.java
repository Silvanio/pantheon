package com.pantheon.service.dto;

import com.pantheon.service.entity.UserProfile;

/** {@code email} always comes from the account itself, not {@link UserProfile} — read-only, never editable here. */
public record UserProfileResponse(String email, String cnpjCpf, String legalName, String address, String postalCode) {

    public static UserProfileResponse from(UserProfile profile, String email) {
        return new UserProfileResponse(
                email, profile.getCnpjCpf(), profile.getLegalName(), profile.getAddress(), profile.getPostalCode());
    }

    /** No {@link UserProfile} row yet — only the account's email is known. */
    public static UserProfileResponse empty(String email) {
        return new UserProfileResponse(email, null, null, null, null);
    }
}
