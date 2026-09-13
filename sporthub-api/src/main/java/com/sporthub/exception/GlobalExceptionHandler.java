package com.sporthub.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // =========================
    // 401 - LOGIN SAI
    // =========================
    @ExceptionHandler({
            BadCredentialsException.class,
            UsernameNotFoundException.class
    })
    public ResponseEntity<Map<String, String>>
    handleBadCredentials(Exception ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        Map.of(
                                "message",
                                "Username hoặc mật khẩu không chính xác"
                        )
                );
    }

    // =========================
    // 401 - ACCOUNT DISABLED
    // =========================
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<Map<String, String>>
    handleDisabled(DisabledException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        Map.of(
                                "message",
                                "Tài khoản hiện không thể đăng nhập"
                        )
                );
    }

    // =========================
    // 400 - @Valid
    // =========================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>
    handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors =
                new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(
                                error.getField(),
                                error.getDefaultMessage()
                        )
                );

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message",
                                "Dữ liệu không hợp lệ",
                                "errors",
                                errors
                        )
                );
    }

    // =========================
    // 400 - JSON SAI FORMAT
    // =========================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>>
    handleInvalidJson(HttpMessageNotReadableException ex) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message",
                                "Dữ liệu JSON không hợp lệ"
                        )
                );
    }

    // =========================
    // 400 - BUSINESS INPUT
    // =========================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>>
    handleIllegalArgument(IllegalArgumentException ex) {

        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "message",
                                ex.getMessage()
                        )
                );
    }

    // =========================
    // 404
    // =========================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>>
    handleNotFound(ResourceNotFoundException ex) {

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                        Map.of(
                                "message",
                                ex.getMessage()
                        )
                );
    }

    // =========================
    // 409
    // =========================
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>>
    handleConflict(ConflictException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        Map.of(
                                "message",
                                ex.getMessage()
                        )
                );
    }

    // =========================
    // 409 - DATABASE CONSTRAINT
    // =========================
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>>
    handleDataIntegrity(DataIntegrityViolationException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                        Map.of(
                                "message",
                                "Dữ liệu đang được sử dụng hoặc vi phạm ràng buộc"
                        )
                );
    }
}