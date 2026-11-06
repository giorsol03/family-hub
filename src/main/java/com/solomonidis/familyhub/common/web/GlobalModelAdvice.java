package com.solomonidis.familyhub.common.web;

import com.solomonidis.familyhub.common.audit.CurrentMemberProvider;
import com.solomonidis.familyhub.member.MemberPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds data every page layout needs: the logged-in member and navigation state. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final CurrentMemberProvider currentMemberProvider;

    public GlobalModelAdvice(CurrentMemberProvider currentMemberProvider) {
        this.currentMemberProvider = currentMemberProvider;
    }

    @ModelAttribute("currentMember")
    public MemberPrincipal currentMember() {
        return currentMemberProvider.currentPrincipal().orElse(null);
    }

    @ModelAttribute("navItems")
    public List<NavItem> navItems() {
        return NavItem.ALL;
    }

    /** First path segment, e.g. "/members/3/edit" -> "members"; "/" -> "home". */
    @ModelAttribute("activeNav")
    public String activeNav(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String[] parts = path.split("/");
        return parts.length > 1 && !parts[1].isBlank() ? parts[1] : "home";
    }
}
