package com.solomonidis.familyhub.member;

import com.solomonidis.familyhub.common.NotFoundException;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Member> findAll() {
        return memberRepository.findAllByOrderByActiveDescNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Member> findActive() {
        return memberRepository.findAllByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Member get(Long id) {
        return memberRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Member " + id + " not found"));
    }

    public Member create(MemberRequest request) {
        String username = normalize(request.username());
        if (!request.hasPassword()) {
            throw new MemberException("password", "Password is required");
        }
        if (memberRepository.existsByUsername(username)) {
            throw new MemberException("username", "Username is already taken");
        }
        Member member = new Member(request.name().trim(), username,
            passwordEncoder.encode(request.password()), request.color().toLowerCase(Locale.ROOT));
        return memberRepository.save(member);
    }

    public Member update(Long id, MemberRequest request) {
        Member member = get(id);
        String username = normalize(request.username());
        if (!username.equals(member.getUsername()) && memberRepository.existsByUsername(username)) {
            throw new MemberException("username", "Username is already taken");
        }
        member.setName(request.name().trim());
        member.setUsername(username);
        member.setColor(request.color().toLowerCase(Locale.ROOT));
        if (request.hasPassword()) {
            member.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return member;
    }

    /** Soft delete: the member can no longer log in, but history ("created by") is kept. */
    public void deactivate(Long id, Long currentMemberId) {
        if (id.equals(currentMemberId)) {
            throw new MemberException(null, "You can't deactivate yourself");
        }
        get(id).setActive(false);
    }

    public void activate(Long id) {
        get(id).setActive(true);
    }

    /** Creates the very first member (from env vars) if the household has none yet. */
    public boolean createBootstrapMemberIfNone(String name, String username, String password) {
        if (memberRepository.count() > 0) {
            return false;
        }
        create(new MemberRequest(name, username, MemberRequest.DEFAULT_COLOR, password));
        return true;
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
