package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskCommentRequest(@NotBlank String body) {
}
