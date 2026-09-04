package com.pantheon.service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** {@code roleDescription} may be omitted when {@code membershipId} is given — the service pre-fills it from that member's construction function/specialty. */
public record WorkforceEntryRequest(UUID membershipId, String roleDescription, @NotNull @Min(1) Integer headcount) {
}
