package com.pantheon.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ScheduleExceptionHandler {

    @ExceptionHandler(ScheduleStageNotFoundException.class)
    public ResponseEntity<String> handleScheduleStageNotFound(ScheduleStageNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(ScheduleTaskNotFoundException.class)
    public ResponseEntity<String> handleScheduleTaskNotFound(ScheduleTaskNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(SelfDependencyException.class)
    public ResponseEntity<String> handleSelfDependency(SelfDependencyException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(DuplicateDependencyException.class)
    public ResponseEntity<String> handleDuplicateDependency(DuplicateDependencyException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }
}
