package com.pantheon.service.controller;

import com.pantheon.service.dto.AcceptInvitationResponse;
import com.pantheon.service.dto.AuthResponse;
import com.pantheon.service.dto.CompleteRegistrationRequest;
import com.pantheon.service.dto.InvitationResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.security.JwtService;
import com.pantheon.service.service.InvitationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token-driven invitation endpoints. {@code GET} and {@code complete-registration} are
 * unauthenticated — the token is the credential. {@code accept} requires a session token
 * belonging to the invited account (enforced in {@link InvitationService}).
 */
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final InvitationService invitationService;
    private final JwtService jwtService;

    public InvitationController(InvitationService invitationService, JwtService jwtService) {
        this.invitationService = invitationService;
        this.jwtService = jwtService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<InvitationResponse> get(@PathVariable String token) {
        return ResponseEntity.ok(invitationService.getByToken(token));
    }

    @PostMapping("/{token}/complete-registration")
    public ResponseEntity<AuthResponse> completeRegistration(
            @PathVariable String token, @Valid @RequestBody CompleteRegistrationRequest request) {
        AppUser user = invitationService.completeRegistration(token, request.password(), request.displayName());
        return ResponseEntity.ok(new AuthResponse(jwtService.issueToken(user.getId(), user.getEmail())));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<AcceptInvitationResponse> accept(
            @PathVariable String token, @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(invitationService.accept(token, user.getId()));
    }
}
