package com.pantheon.service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetFolderTaskLinkRequest(@NotNull UUID taskCardId) {
}
