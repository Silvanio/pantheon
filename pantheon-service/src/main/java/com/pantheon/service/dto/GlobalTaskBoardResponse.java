package com.pantheon.service.dto;

import java.util.List;

public record GlobalTaskBoardResponse(List<TaskColumnResponse> columns, List<GlobalTaskCardResponse> cards) {
}
