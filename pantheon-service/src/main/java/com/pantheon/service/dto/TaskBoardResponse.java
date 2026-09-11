package com.pantheon.service.dto;

import java.util.List;

public record TaskBoardResponse(List<TaskColumnResponse> columns, List<TaskCardResponse> cards) {
}
