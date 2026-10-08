package com.sagares.saga_res_api.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException exception) {
        ErrorResponse response = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", exception.getMessage()
        );
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        ErrorResponse response = new ErrorResponse(
                exception.getStatus().value(),
                exception.getCode(),
                exception.getMessage()
        );

        return ResponseEntity
                .status(exception.getStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed.");

        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "DATA_INTEGRITY_CONFLICT",
                "Request conflicts with existing data."
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
}
