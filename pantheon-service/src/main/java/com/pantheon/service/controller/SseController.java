package com.pantheon.service.controller;

import com.pantheon.service.entity.AppUser;
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

    public SseController(SseBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    // Reaches this handler only if the JWT filter + Spring Security authorized the
    // request (anyRequest().authenticated() in SecurityConfig) - unauthenticated
    // subscription attempts get a 401 before a stream is ever opened.
    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AppUser user) {
        log.info("SSE subscription opened for user {}", user.getEmail());
        return broadcaster.subscribe();
    }
}
