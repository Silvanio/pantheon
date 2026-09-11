package com.pantheon.service.controller;

import com.pantheon.service.dto.FornecedorResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.FornecedorService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FornecedorController {

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    @GetMapping("/api/construction-sites/{siteId}/fornecedores")
    public ResponseEntity<List<FornecedorResponse>> search(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId, @RequestParam String cnpjPrefix) {
        List<FornecedorResponse> fornecedores = fornecedorService
                .searchByCnpjPrefix(siteId, user.getId(), cnpjPrefix)
                .stream()
                .map(FornecedorResponse::from)
                .toList();
        return ResponseEntity.ok(fornecedores);
    }
}
