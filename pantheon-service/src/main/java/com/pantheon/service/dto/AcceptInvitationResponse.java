package com.pantheon.service.dto;

import java.util.UUID;

public record AcceptInvitationResponse(UUID projectId, String projectName) {
}
