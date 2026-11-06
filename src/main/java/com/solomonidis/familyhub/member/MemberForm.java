package com.solomonidis.familyhub.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Add/edit member form. Password is required on create and optional on edit (blank = keep). */
public class MemberForm {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9._-]{3,50}", message = "3–50 characters: letters, digits, dot, dash or underscore")
    private String username;

    @NotBlank
    @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "Pick a colour")
    private String color = "#3b82f6";

    @Pattern(regexp = "^$|.{8,100}", message = "At least 8 characters")
    private String password = "";

    public static MemberForm from(Member member) {
        MemberForm form = new MemberForm();
        form.setName(member.getName());
        form.setUsername(member.getUsername());
        form.setColor(member.getColor());
        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }
}
