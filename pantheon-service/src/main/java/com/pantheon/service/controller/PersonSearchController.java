package com.pantheon.service.controller;

import com.pantheon.service.dto.EmailConflictCheck;
import com.pantheon.service.dto.PersonSearchResult;
import com.pantheon.service.entity.AppUser;
import com.pantheon.service.service.PersonSearchService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sites/{siteId}/people")
public class PersonSearchController {

    private final PersonSearchService personSearchService;

    public PersonSearchController(PersonSearchService personSearchService) {
        this.personSearchService = personSearchService;
    }

    @GetMapping("/search")
    public List<PersonSearchResult> search(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId, @RequestParam("q") String query) {
        return personSearchService.search(siteId, user.getId(), query);
    }

    @GetMapping("/email-check")
    public EmailConflictCheck emailCheck(
            @AuthenticationPrincipal AppUser user, @PathVariable UUID siteId, @RequestParam("email") String email) {
        return personSearchService.checkEmailConflicts(siteId, user.getId(), email);
    }
}
