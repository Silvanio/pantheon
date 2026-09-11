package com.pantheon.service.dto;

import jakarta.validation.constraints.NotBlank;

public record FornecedorRequest(@NotBlank String cnpj, @NotBlank String name, String address, String contactName, String contactPhone) {
}
