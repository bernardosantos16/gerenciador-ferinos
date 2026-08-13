package com.bernardo.geradortimes.shared.api;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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
        log.info("Validacao de entrada falhou - errorType: METHOD_ARGUMENT_NOT_VALID, errors: {}, path: {}",
                errors.stream().map(e -> e.field() + "=" + e.message()).toList(), request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        List<ValidationError> errors = ex.getConstraintViolations().stream()
                .map(ApiExceptionHandler::toValidationError)
                .toList();
        pd.setProperty("errors", toFieldErrors(errors));
        log.info("Validacao de entrada falhou - errorType: CONSTRAINT_VIOLATION, errors: {}, path: {}",
                errors.stream().map(e -> e.field() + "=" + e.message()).toList(), request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        String message = ex.getRequiredType() == null
                ? "invalid value"
                : "invalid value for type " + ex.getRequiredType().getSimpleName();
        pd.setProperty("errors", List.of(new FieldErrorResponse(ex.getName(), message)));
        log.info("Validacao de entrada falhou - errorType: TYPE_MISMATCH, field: {}, path: {}",
                ex.getName(), request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        pd.setProperty("errors", List.of(new FieldErrorResponse(ex.getParameterName(), "parameter is required")));
        log.info("Validacao de entrada falhou - errorType: MISSING_PARAMETER, parameter: {}, path: {}",
                ex.getParameterName(), request.getRequestURI());
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail pd = buildValidationProblemDetail(request);
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof InvalidFormatException invalidFormat) {
            String field = extractJsonField(invalidFormat.getPath());
            pd.setProperty("errors", List.of(new FieldErrorResponse(field, "invalid value")));
            log.info("Corpo da requisicao invalido - errorType: INVALID_FORMAT, field: {}, path: {}",
                    field, request.getRequestURI());
            return pd;
        }
        if (cause instanceof MismatchedInputException mismatchedInput) {
            String field = extractJsonField(mismatchedInput.getPath());
            pd.setProperty("errors", List.of(new FieldErrorResponse(field, "invalid value")));
            log.info("Corpo da requisicao invalido - errorType: MISMATCHED_INPUT, field: {}, path: {}",
                    field, request.getRequestURI());
            return pd;
        }

        pd.setProperty("errors", List.of(new FieldErrorResponse("body", "invalid request body")));
        log.info("Corpo da requisicao invalido - errorType: UNREADABLE_BODY, path: {}", request.getRequestURI());
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
        if (ex.getStatus() == HttpStatus.CONFLICT) {
            log.warn("Requisicao rejeitada por conflito - status: {}, field: {}, path: {}",
                    ex.getStatus(), ex.getField(), request.getRequestURI());
        } else {
            log.info("Requisicao rejeitada por regra de negocio - status: {}, field: {}, path: {}",
                    ex.getStatus(), ex.getField(), request.getRequestURI());
        }
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
        int status = ex.getStatusCode().value();
        if (ex.getStatusCode().is5xxServerError()) {
            log.error("Erro ao processar requisicao - status: {}, path: {}, reason: {}",
                    status, request.getRequestURI(), message, ex);
        } else if (status == HttpStatus.UNAUTHORIZED.value()
                || status == HttpStatus.FORBIDDEN.value()
                || status == HttpStatus.CONFLICT.value()) {
            log.warn("Requisicao rejeitada - status: {}, path: {}, reason: {}",
                    status, request.getRequestURI(), message);
        } else {
            log.debug("Requisicao rejeitada - status: {}, path: {}, reason: {}",
                    status, request.getRequestURI(), message);
        }
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) throws Exception {
        if (ex instanceof org.springframework.security.access.AccessDeniedException
                || ex instanceof org.springframework.security.core.AuthenticationException) {
            throw ex;
        }
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setInstance(java.net.URI.create(request.getRequestURI()));
        pd.setDetail("Unexpected error");
        log.error("Erro inesperado ao processar requisicao - path: {}", request.getRequestURI(), ex);
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
            case "email already registered" -> "login";
            case "invalid or expired verification token" -> "token";
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
