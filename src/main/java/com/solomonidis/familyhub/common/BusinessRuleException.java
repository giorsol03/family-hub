package com.solomonidis.familyhub.common;

import org.springframework.http.HttpStatus;

/**
 * A request that breaks a business rule. {@code field} is the form field to show the message on
 * (or null for a general error). Rendered as a ProblemDetail by ApiExceptionHandler.
 */
public class BusinessRuleException extends RuntimeException {

    private final String field;
    private final HttpStatus status;

    public BusinessRuleException(String field, String message) {
        this(field, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessRuleException(String field, String message, HttpStatus status) {
        super(message);
        this.field = field;
        this.status = status;
    }

    public String getField() {
        return field;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
