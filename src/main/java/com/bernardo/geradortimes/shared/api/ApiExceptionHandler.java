package com.bernardo.geradortimes.shared.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(ex.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toValidationError)
                .toList());
        errors.addAll(ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> new ValidationError("global", error.getDefaultMessage()))
                .toList());
        pd.setProperty("errors", toFieldErrors(errors));
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(ApiExceptionHandler::toValidationError)
                .toList();
        pd.setProperty("errors", toFieldErrors(errors));
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        String message = ex.getRequiredType() == null
                ? "invalid value"
                : "invalid value for type " + ex.getRequiredType().getSimpleName();
        pd.setProperty("errors", List.of(new FieldErrorResponse(ex.getName(), message)));
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        pd.setProperty("errors", List.of(new FieldErrorResponse(ex.getParameterName(), "parameter is required")));
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof InvalidFormatException invalidFormat) {
            String field = extractJsonField(invalidFormat.getPath());
            pd.setProperty("errors", List.of(new FieldErrorResponse(field, "invalid value")));
            return pd;
        }
        if (cause instanceof MismatchedInputException mismatchedInput) {
            String field = extractJsonField(mismatchedInput.getPath());
            pd.setProperty("errors", List.of(new FieldErrorResponse(field, "invalid value")));
            return pd;
        }

        pd.setProperty("errors", List.of(new FieldErrorResponse("body", "invalid request body")));
        return pd;
    }

    @ExceptionHandler(FieldValidationException.class)
    public ProblemDetail handleFieldValidationException(FieldValidationException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(ex.getStatus());
        pd.setInstance(java.net.URI.create(request.getRequestURI()));
        if (ex.getMessage() != null && !ex.getMessage().isBlank()) {
            pd.setDetail(ex.getMessage());
        }
        pd.setProperty("errors", List.of(new FieldErrorResponse(ex.getField(), ex.getMessage())));
        return pd;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(ex.getStatusCode());
        pd.setInstance(java.net.URI.create(request.getRequestURI()));
        if (ex.getReason() != null && !ex.getReason().isBlank()) {
            pd.setDetail(ex.getReason());
        }
        String message = ex.getReason();
        if (message == null || message.isBlank()) {
            message = pd.getTitle();
        }
        String field = mapKnownField(message);
        pd.setProperty("errors", List.of(new FieldErrorResponse(field, message)));
        return pd;
    }

    private static ValidationError toValidationError(FieldError fe) {
        return new ValidationError(fe.getField(), fe.getDefaultMessage());
    }

    private static ValidationError toValidationError(ConstraintViolation<?> cv) {
        String path = cv.getPropertyPath() == null ? null : cv.getPropertyPath().toString();
        String field = extractLeafField(path);
        return new ValidationError(field, cv.getMessage());
    }

    private static String extractLeafField(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < path.length()) {
            return path.substring(lastDot + 1);
        }
        return path;
    }

    private static String extractJsonField(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "body";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : path) {
            if (ref.getFieldName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.isEmpty() ? "body" : sb.toString();
    }

    private static List<FieldErrorResponse> toFieldErrors(List<ValidationError> errors) {
        return errors.stream()
                .map(error -> new FieldErrorResponse(error.field(), error.message()))
                .toList();
    }

    private static String mapKnownField(String message) {
        if (message == null) {
            return "global";
        }
        return switch (message) {
            case "nickname already exists" -> "nickname";
            case "login already exists" -> "login";
            default -> "global";
        };
    }

    private static ProblemDetail buildValidationProblemDetail(HttpServletRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation failed");
        pd.setDetail("Request validation failed. Check the 'errors' property for details.");
        pd.setInstance(java.net.URI.create(request.getRequestURI()));
        return pd;
    }

    public record ValidationError(String field, String message) {
    }

    public record FieldErrorResponse(String field, String message) {
    }
}
