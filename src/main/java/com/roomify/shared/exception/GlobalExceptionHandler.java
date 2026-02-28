package com.roomify.shared.exception;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${api.errors.include-stacktrace:false}")
    private boolean includeStackTrace;

    @ExceptionHandler(ClientApiException.class)
    public ResponseEntity<ApiErrorResponse> handleClientApiException(
            ClientApiException ex,
            HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ApiErrorResponse(
                        ex.getMessage(),
                        request.getRequestURI(),
                        ex.getStatusCode().value(),
                        LocalDateTime.now(),
                        includeStackTrace ? getStackTraceAsString(ex) : null)
                );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        ex.getMessage(),
                        request.getRequestURI(),
                        UNAUTHORIZED.value(),
                        LocalDateTime.now(),
                        includeStackTrace ? getStackTraceAsString(ex) : null)
                );
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimiter(
            RequestNotPermitted ex,
            HttpServletRequest request) {
        return ResponseEntity.status(TOO_MANY_REQUESTS)
                .body(new ApiErrorResponse(
                        ex.getMessage(),
                        request.getRequestURI(),
                        TOO_MANY_REQUESTS.value(),
                        LocalDateTime.now(),
                        includeStackTrace ? getStackTraceAsString(ex) : null)
                );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
        return ResponseEntity.status(BAD_REQUEST)
                .body(new ApiErrorResponse(
                        ex.getMessage(),
                        request.getRequestURI(),
                        BAD_REQUEST.value(),
                        LocalDateTime.now(),
                        includeStackTrace ? getStackTraceAsString(ex) : null)
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(INTERNAL_SERVER_ERROR.value())
                .body(new ApiErrorResponse(
                        ex.getMessage(),
                        request.getRequestURI(),
                        INTERNAL_SERVER_ERROR.value(),
                        LocalDateTime.now(),
                        includeStackTrace ? getStackTraceAsString(ex) : null)
                );
    }

    private String getStackTraceAsString(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }

}
