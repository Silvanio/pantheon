package com.pantheon.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class CompanyExceptionHandler {

    @ExceptionHandler(SiteLimitExceededException.class)
    public ResponseEntity<String> handleSiteLimitExceeded(SiteLimitExceededException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(PlanDowngradeBlockedException.class)
    public ResponseEntity<String> handlePlanDowngradeBlocked(PlanDowngradeBlockedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(NotCompanyAdminException.class)
    public ResponseEntity<String> handleNotCompanyAdmin(NotCompanyAdminException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(NotCompanyMemberException.class)
    public ResponseEntity<String> handleNotCompanyMember(NotCompanyMemberException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<String> handleCompanyNotFound(CompanyNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MemberAlreadyActiveException.class)
    public ResponseEntity<String> handleMemberAlreadyActive(MemberAlreadyActiveException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
