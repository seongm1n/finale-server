package com.finale.finale.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e, HttpServletRequest request) {
        log.error(e.getMessage(), e);
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(new ErrorResponse(e.getErrorCode(), request.getRequestURI()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandler(NoHandlerFoundException e, HttpServletRequest request) {
        return ResponseEntity
                .status(ErrorCode.PATH_NOT_FOUND.getStatus())
                .body(new ErrorResponse(ErrorCode.PATH_NOT_FOUND, request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e, HttpServletRequest
            request) {
        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(new ErrorResponse(ErrorCode.METHOD_NOT_ALLOWED, request.getRequestURI()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e, HttpServletRequest request) {
        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(new ErrorResponse(ErrorCode.INVALID_REQUEST, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ErrorCode.UNKNOWN_ERROR, request.getRequestURI()));
    }
}
