package com.solomonidis.familyhub.member;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberApiController {

    private final MemberService memberService;

    public MemberApiController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public List<MemberResponse> list() {
        return memberService.findAll().stream().map(MemberResponse::of).toList();
    }

    @GetMapping("/{id}")
    public MemberResponse get(@PathVariable Long id) {
        return MemberResponse.of(memberService.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse create(@Valid @RequestBody MemberRequest request) {
        return MemberResponse.of(memberService.create(request));
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable Long id, @Valid @RequestBody MemberRequest request) {
        return MemberResponse.of(memberService.update(id, request));
    }

    @PostMapping("/{id}/deactivate")
    public MemberResponse deactivate(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal me) {
        memberService.deactivate(id, me.id());
        return MemberResponse.of(memberService.get(id));
    }

    @PostMapping("/{id}/activate")
    public MemberResponse activate(@PathVariable Long id) {
        memberService.activate(id);
        return MemberResponse.of(memberService.get(id));
    }
}
