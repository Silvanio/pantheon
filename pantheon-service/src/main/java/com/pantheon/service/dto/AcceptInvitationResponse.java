package com.pantheon.service.dto;

import java.util.UUID;

public record AcceptInvitationResponse(UUID targetId, String targetName, String membershipType) {
}
