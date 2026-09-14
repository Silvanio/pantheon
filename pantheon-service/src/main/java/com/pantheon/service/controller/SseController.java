package com.pantheon.service.controller;

import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.CompanyMembership;
import com.pantheon.service.repository.CompanyMembershipRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

    public SseController(SseBroadcaster broadcaster, CompanyMembershipRepository membershipRepository) {
        this.broadcaster = broadcaster;
        this.membershipRepository = membershipRepository;
    }

    // Reaches this handler only if the JWT filter + Spring Security authorized the
    // request (anyRequest().authenticated() in SecurityConfig) - unauthenticated
    // subscription attempts get a 401 before a stream is ever opened.
    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AppUser user) {
        log.info("SSE subscription opened for user {}", user.getEmail());
        Set<UUID> companyIds = membershipRepository.findByUserId(user.getId()).stream()
                .filter(CompanyMembership::isActive)
                .map(CompanyMembership::getCompanyId)
                .collect(Collectors.toSet());
        return broadcaster.subscribe(companyIds);
    }
}
