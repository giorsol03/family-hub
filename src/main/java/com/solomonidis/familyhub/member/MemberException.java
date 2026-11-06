package com.solomonidis.familyhub.member;

import com.solomonidis.familyhub.common.BusinessRuleException;
import org.springframework.http.HttpStatus;

/** A member change that breaks a business rule. A taken username is a 409 conflict; anything else is 400. */
public class MemberException extends BusinessRuleException {

    public MemberException(String field, String message) {
        super(field, message, "username".equals(field) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST);
    }
}
