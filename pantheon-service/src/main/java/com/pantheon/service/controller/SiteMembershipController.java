package com.pantheon.service.controller;

import com.pantheon.service.dto.AddSiteMemberRequest;
import com.pantheon.service.dto.MemberInvitationResponse;
import com.pantheon.service.dto.SiteMemberResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.service.SiteMembershipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites/{siteId}/members")
public class SiteMembershipController {

    private final SiteMembershipService siteMembershipService;

    public SiteMembershipController(SiteMembershipService siteMembershipService) {
        this.siteMembershipService = siteMembershipService;
    }

    @PostMapping
    public ResponseEntity<?> add(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID siteId,
            @Valid @RequestBody AddSiteMemberRequest request) {
        if (request.function() == ConstructionFunction.SERVICE_PROVIDER && request.email() == null) {
            SiteMembership membership = siteMembershipService.addAccountlessServiceProvider(
                    siteId, user.getId(), request.displayName(), request.trade(), request.contactEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(membership));
        }

        MembershipInvitation invitation = siteMembershipService.inviteMember(
                siteId, user.getId(), request.function(), request.email(), request.cpf());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MemberInvitationResponse(
                invitation.getId(), invitation.getMembershipId(), invitation.getEmail(),
                invitation.isRequiresRegistration()));
    }

    @GetMapping
    public ResponseEntity<List<SiteMemberResponse>> list(@AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        return ResponseEntity.ok(siteMembershipService.listMembers(siteId, user.getId()));
    }

    private SiteMemberResponse toResponse(SiteMembership m) {
        return new SiteMemberResponse(
                m.getId(), m.getUserId(), m.getContactEmail(), m.getDisplayName(), m.getFunction(),
                m.getServiceProviderTrade(), m.getClientCpf(), m.getStatus(), false);
    }
}
