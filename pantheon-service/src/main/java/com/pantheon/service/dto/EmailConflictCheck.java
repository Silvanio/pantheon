package com.pantheon.service.dto;

/**
 * Whether an email is already blocked from being added to a construction site's team: already
 * an active member of that exact site, or already tied to a person with a membership in a
 * different company. Reveals existence only — never the other company's identity.
 */
public record EmailConflictCheck(boolean activeOnThisSite, boolean existsInAnotherCompany) {
}
