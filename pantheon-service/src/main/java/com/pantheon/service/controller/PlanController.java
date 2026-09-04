package com.pantheon.service.controller;

import com.pantheon.service.dto.CompanyResponse;
import com.pantheon.service.dto.PlanResponse;
import com.pantheon.service.dto.PlanSelectionRequest;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Company;
import com.pantheon.service.service.PlanService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/api/plans")
    public ResponseEntity<List<PlanResponse>> list() {
        return ResponseEntity.ok(planService.listPlans().stream().map(PlanResponse::from).toList());
    }

    @PutMapping("/api/companies/{id}/plan")
    public ResponseEntity<CompanyResponse> selectPlan(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id, @Valid @RequestBody PlanSelectionRequest request) {
        Company company = planService.selectOrChangePlan(id, user.getId(), request.planCode());
        return ResponseEntity.ok(CompanyResponse.from(company));
    }
}
