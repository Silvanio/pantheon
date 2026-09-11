package com.pantheon.service.dto;

import com.pantheon.service.entity.TaskLabel;
import java.util.UUID;

public record TaskLabelResponse(UUID id, UUID constructionSiteId, String name, String colorHex) {

    public static TaskLabelResponse from(TaskLabel label) {
        return new TaskLabelResponse(label.getId(), label.getConstructionSiteId(), label.getName(), label.getColorHex());
    }
}
