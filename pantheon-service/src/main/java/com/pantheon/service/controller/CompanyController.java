package com.pantheon.service.controller;

import com.pantheon.service.dto.AddCompanyMemberRequest;
import com.pantheon.service.dto.CompanyMemberResponse;
import com.pantheon.service.dto.CompanyMembershipResponse;
import com.pantheon.service.dto.CompanyRegistrationRequest;
import com.pantheon.service.dto.CompanyResponse;
import com.pantheon.service.dto.MemberInvitationResponse;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.entity.Company;
import com.pantheon.service.entity.MembershipInvitation;
import com.pantheon.service.service.CompanyService;
import com.pantheon.service.storage.StorageKeys;
import com.pantheon.service.storage.StorageService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;
    private final StorageService storageService;

    public CompanyController(CompanyService companyService, StorageService storageService) {
        this.companyService = companyService;
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<CompanyResponse> create(
            @AuthenticationPrincipal AppUser user, @Valid @RequestBody CompanyRegistrationRequest request) {
        Company company = companyService.create(user.getId(), request.companyName());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyResponse.from(company));
    }

    @GetMapping("/me")
    public ResponseEntity<List<CompanyMembershipResponse>> myCompanies(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(companyService.listMyCompanies(user.getId()));
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<CompanyResponse> completeProfile(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestParam String legalName,
            @RequestParam String tradeName,
            @RequestParam String cnpj,
            @RequestParam String address,
            @RequestPart MultipartFile logo) {
        String extension = extensionOf(logo.getOriginalFilename());
        String key = StorageKeys.companyLogoKey(id, extension);
        storageService.putObject(key, readBytes(logo), logo.getContentType());

        Company company = companyService.completeProfile(id, user.getId(), legalName, tradeName, cnpj, address, key);
        return ResponseEntity.ok(CompanyResponse.from(company));
    }

    @PostMapping("/{id}/staff")
    public ResponseEntity<MemberInvitationResponse> addStaffMember(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody AddCompanyMemberRequest request) {
        MembershipInvitation invitation = companyService.addStaffMember(id, user.getId(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MemberInvitationResponse(
                invitation.getId(), invitation.getMembershipId(), invitation.getEmail(),
                invitation.isRequiresRegistration()));
    }

    @GetMapping("/{id}/staff")
    public ResponseEntity<List<CompanyMemberResponse>> listStaff(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID id) {
        return ResponseEntity.ok(companyService.listStaff(id, user.getId()));
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
