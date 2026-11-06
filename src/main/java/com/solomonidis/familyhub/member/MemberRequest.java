package com.solomonidis.familyhub.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create/update member payload. Password is required on create and optional on update (blank/null = keep). */
public record MemberRequest(
    @NotBlank @Size(max = 100)
    String name,

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "3–50 characters: letters, digits, dot, dash or underscore")
    String username,

    @NotBlank
    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Must be a hex colour like #3b82f6")
    String color,

    @Pattern(regexp = "^$|.{8,100}", message = "At least 8 characters")
    String password
) {

    public static final String DEFAULT_COLOR = "#3b82f6";

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }
}
