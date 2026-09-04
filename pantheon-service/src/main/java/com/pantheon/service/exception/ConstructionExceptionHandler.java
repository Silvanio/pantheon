package com.pantheon.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ConstructionExceptionHandler {

    @ExceptionHandler(ConstructionSiteNotFoundException.class)
    public ResponseEntity<String> handleConstructionSiteNotFound(ConstructionSiteNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(NotProjectMemberException.class)
    public ResponseEntity<String> handleNotProjectMember(NotProjectMemberException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }

    @ExceptionHandler(EquipmentNotFoundException.class)
    public ResponseEntity<String> handleEquipmentNotFound(EquipmentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(NotConstructionSiteManagerException.class)
    public ResponseEntity<String> handleNotConstructionSiteManager(NotConstructionSiteManagerException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    }
}
