package com.pantheon.service.controller;

import com.pantheon.service.dto.AddSiteMemberRequest;
import com.pantheon.service.dto.MemberInvitationResponse;
import com.pantheon.service.dto.SiteMemberResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.ConstructionFunction;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.entity.SiteMembership;
import com.pantheon.service.exception.EmailRequiredException;
import com.pantheon.service.service.SiteMembershipService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
        boolean accountless = request.function() == ConstructionFunction.SERVICE_PROVIDER && request.email() == null;
        if (accountless) {
            SiteMembership membership = siteMembershipService.addAccountlessServiceProvider(
                    siteId, user.getId(), request.displayName(), request.trade(), request.contactEmail(),
                    request.cpf(), request.phone());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(membership));
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new EmailRequiredException();
        }

        MembershipInvitation invitation = siteMembershipService.inviteMember(
                siteId, user.getId(), request.function(), request.displayName(), request.email(), request.cpf(),
                request.phone());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MemberInvitationResponse(
                invitation.getId(), invitation.getMembershipId(), invitation.getEmail(),
                invitation.isRequiresRegistration()));
    }

    @GetMapping
    public ResponseEntity<List<SiteMemberResponse>> list(@AuthenticationPrincipal AppUser user, @PathVariable UUID siteId) {
        return ResponseEntity.ok(siteMembershipService.listMembers(siteId, user.getId()));
    }

    @DeleteMapping("/{membershipId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId, @PathVariable UUID membershipId) {
        siteMembershipService.removeMember(siteId, user.getId(), membershipId);
        return ResponseEntity.noContent().build();
    }

    private SiteMemberResponse toResponse(SiteMembership m) {
        return new SiteMemberResponse(
                m.getId(), m.getUserId(), m.getContactEmail(), m.getDisplayName(), m.getFunction(),
                m.getServiceProviderTrade(), m.getCpf(), m.getPhone(), m.getStatus(), false);
    }
}
