package com.pantheon.service.dto;

import java.time.LocalDate;

public record UpdateTaskCardDueDateRequest(LocalDate dueDate) {
}
