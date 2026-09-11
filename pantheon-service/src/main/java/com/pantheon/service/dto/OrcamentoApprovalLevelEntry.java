package com.pantheon.service.dto;

import com.pantheon.service.entity.ConstructionFunction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrcamentoApprovalLevelEntry(@Positive int stepOrder, @NotNull ConstructionFunction approverFunction) {
}
