package com.pantheon.service.controller;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.repository.CompanyMembershipRepository;
import com.pantheon.service.repository.ConstructionSiteRepository;
import com.pantheon.service.repository.SiteMembershipRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.pantheon.service.sse.SseBroadcaster;
@RestController
@RequestMapping("/api/sse")
public class SseController {

    private static final Logger log = LoggerFactory.getLogger(SseController.class);

    private final SseBroadcaster broadcaster;
    private final CompanyMembershipRepository membershipRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final ConstructionSiteRepository siteRepository;

    public SseController(
            SseBroadcaster broadcaster, CompanyMembershipRepository membershipRepository,
            SiteMembershipRepository siteMembershipRepository, ConstructionSiteRepository siteRepository) {
        this.broadcaster = broadcaster;
        this.membershipRepository = membershipRepository;
        this.siteMembershipRepository = siteMembershipRepository;
        this.siteRepository = siteRepository;
    }

    // Reaches this handler only if the JWT filter + Spring Security authorized the
    // request (anyRequest().authenticated() in SecurityConfig) - unauthenticated
    // subscription attempts get a 401 before a stream is ever opened.
    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AppUser user) {
        log.info("SSE subscription opened for user {}", user.getEmail());
        return broadcaster.subscribe(resolveCompanyIds(user.getId()));
    }

    /**
     * A user's company scope for SSE purposes is not just their {@code CompanyMembership}
     * (internal staff) — a site-only member (client, architect, engineer, foreman, service
     * provider with a {@code SiteMembership} but no company staff role) also needs company-scoped
     * events for the obra boards they can access. See {@code SiteAccessService}, which grants the
     * same dual path for REST access.
     */
    private Set<UUID> resolveCompanyIds(UUID userId) {
        Set<UUID> companyIds = new HashSet<>();
        membershipRepository.findByUserId(userId).stream()
                .filter(CompanyMembership::isActive)
                .map(CompanyMembership::getCompanyId)
                .forEach(companyIds::add);

        List<UUID> memberSiteIds = siteMembershipRepository.findByUserId(userId).stream()
                .filter(SiteMembership::isActive)
                .map(SiteMembership::getConstructionSiteId)
                .toList();
        siteRepository.findAllById(memberSiteIds).forEach(site -> companyIds.add(site.getCompanyId()));

        return companyIds;
    }
}
