package com.pantheon.service.sse;

public class SseEventPublishException extends RuntimeException {

    public SseEventPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
