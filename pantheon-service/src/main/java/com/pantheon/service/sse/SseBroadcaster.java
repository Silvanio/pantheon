package com.pantheon.service.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Registry of connected SSE clients and the internal event bus that pushes application
 * events to them. Independent of the RabbitMQ producer (see design.md) — this pushes to
 * browsers; RabbitMQ ({@code SseEventListener}) is what rehydrates events produced on a
 * different pantheon-service instance into this instance's local emitters.
 *
 * <p>Every emitter is kept in {@code allEmitters} (used for unscoped events, e.g.
 * {@code user-registered}, and for the keep-alive sweep) and, additionally, indexed by each
 * company its owner belongs to in {@code emittersByCompany} (used for company-scoped events,
 * e.g. {@code task-card-moved}) so a scoped broadcast never touches emitters outside that
 * company.
 */
@Component
public class SseBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(SseBroadcaster.class);
    private static final long HEARTBEAT_INTERVAL_MS = 20_000;

    private final Set<SseEmitter> allEmitters = new CopyOnWriteArraySet<>();
    private final Map<UUID, Set<SseEmitter>> emittersByCompany = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Set<UUID> companyIds) {
        SseEmitter emitter = new SseEmitter(0L);
        allEmitters.add(emitter);
        for (UUID companyId : companyIds) {
            emittersByCompany.computeIfAbsent(companyId, id -> new CopyOnWriteArraySet<>()).add(emitter);
        }

        Runnable cleanup = () -> {
            allEmitters.remove(emitter);
            for (UUID companyId : companyIds) {
                emittersByCompany.getOrDefault(companyId, Collections.emptySet()).remove(emitter);
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        return emitter;
    }

    /** Delivers an unscoped event to every connected client. */
    public void broadcast(String eventName, Object data) {
        send(allEmitters, eventName, data);
    }

    /** Delivers an event only to clients belonging to the given company. */
    public void broadcastToCompany(UUID companyId, String eventName, Object data) {
        send(emittersByCompany.getOrDefault(companyId, Collections.emptySet()), eventName, data);
    }

    /** Keeps idle connections open across intermediary proxies/load balancers. */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MS)
    void sendHeartbeats() {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : allEmitters) {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (IOException e) {
                log.debug("Dropping SSE emitter after failed heartbeat", e);
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            allEmitters.removeAll(deadEmitters);
        }
    }

    private void send(Set<SseEmitter> targets, String eventName, Object data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : targets) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.debug("Dropping SSE emitter after failed send", e);
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            targets.removeAll(deadEmitters);
            allEmitters.removeAll(deadEmitters);
        }
    }
}
