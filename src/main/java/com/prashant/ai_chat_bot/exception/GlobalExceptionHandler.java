package com.prashant.ai_chat_bot.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnhandled(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception for path {}", request.getRequestURI(), ex);
    return buildErrorResponse(
      HttpStatus.INTERNAL_SERVER_ERROR,
      "INTERNAL_SERVER_ERROR",
      "Something went wrong while processing your request. Please try again.",
      request
    );
  }

  private ResponseEntity<Map<String, Object>> buildErrorResponse(
    HttpStatus status,
    String code,
    String message,
    HttpServletRequest request
  ) {
    Map<String, Object> payload = Map.of(
      "timestamp", Instant.now().toString(),
      "status", status.value(),
      "error", status.getReasonPhrase(),
      "code", code,
      "message", message,
      "path", request.getRequestURI()
    );
    return ResponseEntity.status(status).body(payload);
  }
}
