package com.pantheon.service.dto;

import com.pantheon.service.entity.TaskLabel;
import java.util.UUID;

public record TaskLabelResponse(UUID id, UUID companyId, UUID cardId, String name, String colorHex) {

    public static TaskLabelResponse from(TaskLabel label) {
        return new TaskLabelResponse(
                label.getId(), label.getCompanyId(), label.getCardId(), label.getName(), label.getColorHex());
    }
}
