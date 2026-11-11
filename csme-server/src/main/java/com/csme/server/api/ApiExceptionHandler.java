package com.csme.server.api;

import com.csme.server.domain.error.AuthenticationFailedException;
import com.csme.server.domain.error.BusinessRuleException;
import com.csme.server.domain.error.ConflictException;
import com.csme.server.domain.error.DomainException;
import com.csme.server.domain.error.NotFoundException;
import com.csme.server.domain.error.ValidationException;
import com.csme.shared.api.ApiError;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduction des erreurs en réponses {@link ApiError}. Aucune trace technique n'est renvoyée au client :
 * les erreurs inattendues reçoivent un identifiant à rechercher dans les journaux du serveur.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ApiError> validation(ValidationException e) {
        return build(HttpStatus.BAD_REQUEST, e.code(), e.getMessage(), e.fieldErrors());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.code(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException e) {
        return build(HttpStatus.CONFLICT, e.code(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> rule(BusinessRuleException e) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, e.code(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ApiError> authentication(AuthenticationFailedException e) {
        return build(HttpStatus.UNAUTHORIZED, e.code(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ApiError> domain(DomainException e) {
        return build(HttpStatus.BAD_REQUEST, e.code(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException e) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Vous n'avez pas l'autorisation d'effectuer cette opération.", Map.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> optimisticLock(OptimisticLockingFailureException e) {
        return build(HttpStatus.CONFLICT, "STALE_DATA",
                "Ces données ont été modifiées entre-temps par un autre utilisateur. Rechargez puis réessayez.", Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e) {
        String traceId = traceId();
        log.warn("Violation d'intégrité [{}] : {}", traceId, e.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(new ApiError(Instant.now(), 409, "INTEGRITY_VIOLATION",
                "L'opération entre en conflit avec des données existantes.", Map.of(), traceId), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> badRequest(Exception e) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Requête mal formée.", Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException e) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Méthode non autorisée.", Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(NoResourceFoundException e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ressource introuvable.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        String traceId = traceId();
        log.error("Erreur inattendue [{}]", traceId, e);
        return new ResponseEntity<>(new ApiError(Instant.now(), 500, "INTERNAL_ERROR",
                "Une erreur inattendue est survenue. Référence : " + traceId, Map.of(), traceId),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static ResponseEntity<ApiError> build(HttpStatus status, String code, String message, Map<String, String> fields) {
        return new ResponseEntity<>(new ApiError(Instant.now(), status.value(), code, message, fields, null), status);
    }

    private static String traceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
