package com.pantheon.service.dto;

import jakarta.validation.Valid;
import java.util.List;

public record PurchaseRequestCreationRequest(@Valid List<PurchaseRequestItemCreationRequest> items) {
}
