package com.solomonidis.familyhub.common.auth;

import com.solomonidis.familyhub.member.MemberPrincipal;
import com.solomonidis.familyhub.member.MemberResponse;
import com.solomonidis.familyhub.member.MemberService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login and logout themselves are handled by Spring Security filters
 * ({@code POST /api/auth/login}, {@code POST /api/auth/logout}); see SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    /** The logged-in member, read fresh from the DB so name/colour edits show immediately. */
    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal MemberPrincipal me) {
        return MemberResponse.of(memberService.get(me.id()));
    }

    /** Touching the token makes Spring write the XSRF-TOKEN cookie; the SPA calls this when the cookie is missing. */
    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
}
