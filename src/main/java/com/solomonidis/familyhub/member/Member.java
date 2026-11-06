package com.solomonidis.familyhub.member;

import com.solomonidis.familyhub.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "member")
public class Member extends AuditableEntity {

    @Column(nullable = false, length = 100)
    private String name;

    /** Stored lower-case so logins are case-insensitive. */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Hex colour such as {@code #3b82f6}, used for badges. */
    @Column(nullable = false, length = 7)
    private String color;

    @Column(nullable = false)
    private boolean active = true;

    protected Member() {
        // for JPA
    }

    public Member(String name, String username, String passwordHash, String color) {
        this.name = name;
        this.username = username;
        this.passwordHash = passwordHash;
        this.color = color;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
