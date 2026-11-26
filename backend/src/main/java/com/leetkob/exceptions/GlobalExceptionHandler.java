package com.leetkob.exceptions;

import com.leetkob.dtos.ApiResponse;
import com.leetkob.exceptions.problem.ProblemException;
import com.leetkob.exceptions.problem.ProblemKind;
import com.leetkob.exceptions.problem.ProblemResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final ProblemResponseWriter problemResponseWriter;

  @ExceptionHandler(ProblemException.class)
  public void handleProblemException(
      ProblemException ex, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String uri = request.getRequestURI();
    String instance = (uri != null && uri.startsWith("/v1/")) ? uri : "/v1";
    problemResponseWriter.writeProblem(request, response, ex.kind(), ex.errors(), instance);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
      NoResourceFoundException ex, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String uri = request.getRequestURI();
    if (uri != null && uri.startsWith("/v1/")) {
      problemResponseWriter.writeProblem(request, response, ProblemKind.NOT_FOUND, null, "/v1");
      return null;
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error("Resource not found"));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
      HttpMessageNotReadableException ex, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    String uri = request.getRequestURI();
    if (uri != null && uri.startsWith("/v1/")) {
      problemResponseWriter.writeProblem(request, response, ProblemKind.INVALID_JSON, null, uri);
      return null;
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("Malformed JSON request body"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
      MethodArgumentTypeMismatchException ex,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    String uri = request.getRequestURI();
    if (uri != null && uri.startsWith("/v1/")) {
      problemResponseWriter.writeProblem(request, response, ProblemKind.NOT_FOUND, null, "/v1");
      return null;
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("Invalid request parameter"));
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ApiResponse<Void>> handleEmailAlreadyExists(
      EmailAlreadyExistsException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(
      InvalidCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ApiResponse.error("Invalid email or password"));
  }

  @ExceptionHandler(OAuthAccountException.class)
  public ResponseEntity<ApiResponse<Void>> handleOAuthAccount(OAuthAccountException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
      MethodArgumentNotValidException ex, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            error -> {
              String field = ((FieldError) error).getField();
              String message = error.getDefaultMessage();
              errors.put(field, message);
            });
    String uri = request.getRequestURI();
    if (uri != null && uri.startsWith("/v1/")) {
      java.util.List<com.leetkob.dtos.publish.FieldErrorItem> fieldErrors =
          errors.entrySet().stream()
              .map(
                  e ->
                      com.leetkob.dtos.publish.FieldErrorItem.forPointer(
                          "/" + e.getKey(), e.getValue()))
              .toList();
      problemResponseWriter.writeProblem(
          request, response, ProblemKind.VALIDATION, fieldErrors, uri);
      return null;
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("Validation failed", errors));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGenericException(
      Exception ex, HttpServletRequest request, HttpServletResponse response) throws IOException {
    String uri = request.getRequestURI();
    if (uri != null && uri.startsWith("/v1/")) {
      problemResponseWriter.writeProblem(request, response, ProblemKind.INTERNAL, null, uri);
      return null;
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("Internal server error: " + ex.getMessage()));
  }
}
