package com.pantheon.service.service;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.exception.NotSuperAdminException;
import com.pantheon.service.repository.AppUserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Single entry point for "is this user a platform superadmin" — a flag on {@link AppUser}
 * independent of any {@code CompanyMembership}/{@code SiteMembership}. Used both to gate
 * superadmin-only endpoints (via {@link #requireSuperAdmin}) and as the bypass check inside
 * {@code SiteAccessService}/{@code CompanyService}/{@code ConstructionSiteService}'s own
 * membership gates, so a superadmin needs no membership row anywhere to see everything.
 */
@Service
public class PlatformAdminService {

    private final AppUserRepository userRepository;

    public PlatformAdminService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isSuperAdmin(UUID userId) {
        return userRepository.findById(userId).map(AppUser::isSuperAdmin).orElse(false);
    }

    public void requireSuperAdmin(UUID userId) {
        if (!isSuperAdmin(userId)) {
            throw new NotSuperAdminException(userId);
        }
    }
}
