package com.feedstartup.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenException(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflictException(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
    }

    // {error: "That username is already taken.", fields: {username: "Already taken"}}
    @ExceptionHandler(AlreadyTakenException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyTakenException(AlreadyTakenException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getFields().forEach(field -> fields.put(field, "Already taken"));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage(), "fields", fields));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(Map.of("error", "The uploaded file is too large"));
    }

    // Malformed JSON body, or a path/query value of the wrong type (e.g. /events/abc).
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> handleBadInput(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "The request could not be read - check the values sent"));
    }

    // A path no controller or static resource serves (e.g. /api/does-not-exist) - without this the
    // catch-all below would answer it as a 500.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, String>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found"));
    }

    // A required parameter or file left out (e.g. the admin publications list without ?year=, or an
    // upload with no file) is the caller's mistake, not a server error.
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Map<String, String>> handleMissingInput(Exception ex) {
        String name = ex instanceof MissingServletRequestParameterException missing
                ? missing.getParameterName()
                : ((MissingServletRequestPartException) ex).getRequestPartName();
        return ResponseEntity.badRequest().body(Map.of("error", "\"" + name + "\" is required"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex, HttpServletResponse response) {
        // File-streaming endpoints (e.g. PublicationController#streamPdf) set the Content-Type
        // to application/pdf before writing the body. If the client cancels an in-flight
        // byte-range request - completely normal for a PDF viewer opening several overlapping
        // ranges - or any other error interrupts that stream, we land here with the response's
        // Content-Type already locked to something a JSON body can't be written as. Trying
        // anyway blows up with "No converter for HashMap with preset Content-Type" and corrupts
        // the in-flight response instead of just letting the cancelled/broken connection be.
        if (response.isCommitted()) {
            return null;
        }
        if (response.getContentType() != null) {
            response.reset();
        }
        // The details stay in the server log: an unexpected exception's message can carry SQL,
        // table names or file paths, none of which the browser should see.
        log.error("Unhandled error", ex);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Something went wrong on the server. Please try again.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
