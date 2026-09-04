package com.pantheon.message.security;

public interface ApiTokenValidator {

    boolean isValid(String token);
}
