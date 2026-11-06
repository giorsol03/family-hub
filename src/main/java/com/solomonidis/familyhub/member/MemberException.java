package com.solomonidis.familyhub.member;

/** A member change that breaks a business rule; {@code field} is the form field to show it on (or null). */
public class MemberException extends RuntimeException {

    private final String field;

    public MemberException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
