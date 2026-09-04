package com.pantheon.service.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pantheon.service.entity.UserProfile;
import com.pantheon.service.repository.UserProfileRepository;
@Service
public class UserProfileService {

    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UserProfile upsert(UUID userId, String cnpjCpf, String legalName, String address, String postalCode) {
        Instant now = Instant.now();
        return repository
                .findByUserId(userId)
                .map(existing -> {
                    existing.update(cnpjCpf, legalName, address, postalCode, now);
                    return existing;
                })
                .orElseGet(() -> repository.save(
                        new UserProfile(UUID.randomUUID(), userId, cnpjCpf, legalName, address, postalCode, now)));
    }
}
