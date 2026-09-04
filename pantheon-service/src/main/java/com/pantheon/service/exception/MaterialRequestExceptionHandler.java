package com.pantheon.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MaterialRequestExceptionHandler {

    @ExceptionHandler(MaterialRequestNotFoundException.class)
    public ResponseEntity<String> handleNotFound(MaterialRequestNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MaterialRequestItemNotFoundException.class)
    public ResponseEntity<String> handleItemNotFound(MaterialRequestItemNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(MaterialRequestNotPendingException.class)
    public ResponseEntity<String> handleNotPending(MaterialRequestNotPendingException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(MaterialRequestNotApprovedException.class)
    public ResponseEntity<String> handleNotApproved(MaterialRequestNotApprovedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @ExceptionHandler(DuplicateReceiptVerificationException.class)
    public ResponseEntity<String> handleDuplicateVerification(DuplicateReceiptVerificationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
