package com.solomonidis.familyhub.member;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The logged-in member as stored in the security context (kept small and serializable). */
public record MemberPrincipal(Long id, String name, String color, String username, String passwordHash, boolean active)
        implements UserDetails, Serializable {

    static MemberPrincipal of(Member member) {
        return new MemberPrincipal(member.getId(), member.getName(), member.getColor(),
            member.getUsername(), member.getPasswordHash(), member.isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
