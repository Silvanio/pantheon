package com.pantheon.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MaterialRequestCreationRequest(@NotEmpty @Valid List<MaterialRequestItemCreationRequest> items) {
}
