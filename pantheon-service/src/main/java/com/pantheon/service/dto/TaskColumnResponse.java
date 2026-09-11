package com.pantheon.service.dto;

import com.pantheon.service.entity.TaskColumn;
import java.util.UUID;

public record TaskColumnResponse(UUID id, UUID companyId, String name, int sortOrder) {

    public static TaskColumnResponse from(TaskColumn column) {
        return new TaskColumnResponse(column.getId(), column.getCompanyId(), column.getName(), column.getSortOrder());
    }
}
