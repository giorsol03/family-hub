package com.solomonidis.familyhub;

import static org.assertj.core.api.Assertions.assertThat;

import com.solomonidis.familyhub.member.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Context starts, Flyway migrations apply on H2, and Hibernate's schema validation passes. */
@SpringBootTest
@ActiveProfiles("test")
class FamilyHubApplicationTests {

    @Autowired
    MemberRepository memberRepository;

    @Test
    void contextLoadsAndBootstrapMemberExists() {
        assertThat(memberRepository.findByUsername("admin"))
            .hasValueSatisfying(admin -> {
                assertThat(admin.isActive()).isTrue();
                assertThat(admin.getCreatedAt()).isNotNull();
                assertThat(admin.getCreatedBy()).isNull();
            });
    }
}
