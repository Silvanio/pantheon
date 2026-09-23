package com.pantheon.service.dto;

import java.util.List;

/** Shared shape for the Pedido de Compra/Orçamentos/Tasks/Projetos sections of the obra summary. */
public record CountStatResponse(long total, List<RecentItemResponse> recent) {
}
