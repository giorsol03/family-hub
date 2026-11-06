package com.solomonidis.familyhub.common.audit;

import com.solomonidis.familyhub.member.Member;
import com.solomonidis.familyhub.member.MemberPrincipal;
import com.solomonidis.familyhub.member.MemberRepository;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the logged-in member; also feeds {@code createdBy} auditing. */
@Component
public class CurrentMemberProvider implements AuditorAware<Member> {

    private final MemberRepository memberRepository;

    public CurrentMemberProvider(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Optional<MemberPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof MemberPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public Optional<Long> currentMemberId() {
        return currentPrincipal().map(MemberPrincipal::id);
    }

    @Override
    public Optional<Member> getCurrentAuditor() {
        return currentMemberId().map(memberRepository::getReferenceById);
    }
}
