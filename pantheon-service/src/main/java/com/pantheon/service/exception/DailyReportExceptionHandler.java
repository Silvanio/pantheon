package com.pantheon.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class DailyReportExceptionHandler {

    @ExceptionHandler(DailyReportNotFoundException.class)
    public ResponseEntity<String> handleDailyReportNotFound(DailyReportNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(DuplicateDailyReportException.class)
    public ResponseEntity<String> handleDuplicateDailyReport(DuplicateDailyReportException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(DailyReportNotEditableException.class)
    public ResponseEntity<String> handleDailyReportNotEditable(DailyReportNotEditableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(DailyReportNotApprovedException.class)
    public ResponseEntity<String> handleDailyReportNotApproved(DailyReportNotApprovedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(DailyReportNotDeletableException.class)
    public ResponseEntity<String> handleDailyReportNotDeletable(DailyReportNotDeletableException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<String> handleInvalidFile(InvalidFileException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(DailyReportCoreFieldsRequiredException.class)
    public ResponseEntity<String> handleDailyReportCoreFieldsRequired(DailyReportCoreFieldsRequiredException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(NoPendingDailyReportApprovalStepException.class)
    public ResponseEntity<String> handleNoPendingDailyReportApprovalStep(NoPendingDailyReportApprovalStepException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(NotCurrentDailyReportApprovalStepException.class)
    public ResponseEntity<String> handleNotCurrentDailyReportApprovalStep(NotCurrentDailyReportApprovalStepException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
}
