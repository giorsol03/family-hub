package com.solomonidis.familyhub.common.web;

import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.member.MemberException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns errors into RFC 7807 {@link ProblemDetail}s. Field-level problems go in a
 * {@code fieldErrors} map ({@code field -> message}) that the frontend shows on the form inputs.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Please fix the highlighted fields");
        problem.setTitle("Validation failed");
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(MemberException.class)
    public ProblemDetail handleMember(MemberException ex) {
        boolean conflict = "username".equals(ex.getField());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            conflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST, ex.getMessage());
        if (ex.getField() != null) {
            problem.setProperty("fieldErrors", Map.of(ex.getField(), ex.getMessage()));
        }
        return problem;
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
    }
}
