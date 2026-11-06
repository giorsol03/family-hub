package com.solomonidis.familyhub.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberServiceTest {

    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void createHashesPasswordAndNormalisesUsername() {
        Member member = memberService.create(form("Maria", "  Maria.S ", "#FF0000", "secret-123"));

        assertThat(member.getId()).isNotNull();
        assertThat(member.getUsername()).isEqualTo("maria.s");
        assertThat(member.getColor()).isEqualTo("#ff0000");
        assertThat(member.getPasswordHash()).isNotEqualTo("secret-123");
        assertThat(passwordEncoder.matches("secret-123", member.getPasswordHash())).isTrue();
    }

    @Test
    void createRejectsDuplicateUsernameIgnoringCase() {
        memberService.create(form("Nikos", "nikos", "#00ff00", "secret-123"));

        assertThatThrownBy(() -> memberService.create(form("Other", "NIKOS", "#0000ff", "secret-456")))
            .isInstanceOf(MemberException.class)
            .hasMessageContaining("taken");
    }

    @Test
    void createRequiresPassword() {
        assertThatThrownBy(() -> memberService.create(form("Eleni", "eleni", "#00ff00", "")))
            .isInstanceOf(MemberException.class)
            .extracting("field").isEqualTo("password");
    }

    @Test
    void updateWithBlankPasswordKeepsExistingPassword() {
        Member member = memberService.create(form("Eleni", "eleni", "#00ff00", "secret-123"));
        String originalHash = member.getPasswordHash();

        memberService.update(member.getId(), form("Eleni S", "eleni", "#123456", ""));

        Member updated = memberService.get(member.getId());
        assertThat(updated.getName()).isEqualTo("Eleni S");
        assertThat(updated.getColor()).isEqualTo("#123456");
        assertThat(updated.getPasswordHash()).isEqualTo(originalHash);
    }

    @Test
    void deactivateAndReactivate() {
        Member member = memberService.create(form("Kostas", "kostas", "#00ff00", "secret-123"));
        Long adminId = memberRepository.findByUsername("admin").orElseThrow().getId();

        memberService.deactivate(member.getId(), adminId);
        assertThat(memberService.get(member.getId()).isActive()).isFalse();

        memberService.activate(member.getId());
        assertThat(memberService.get(member.getId()).isActive()).isTrue();
    }

    @Test
    void cannotDeactivateYourself() {
        Long adminId = memberRepository.findByUsername("admin").orElseThrow().getId();

        assertThatThrownBy(() -> memberService.deactivate(adminId, adminId))
            .isInstanceOf(MemberException.class);
        assertThat(memberService.get(adminId).isActive()).isTrue();
    }

    @Test
    void bootstrapDoesNothingWhenMembersExist() {
        assertThat(memberService.createBootstrapMemberIfNone("X", "someone", "secret-123")).isFalse();
        assertThat(memberRepository.findByUsername("someone")).isEmpty();
    }

    private static MemberRequest form(String name, String username, String color, String password) {
        return new MemberRequest(name, username, color, password);
    }
}
