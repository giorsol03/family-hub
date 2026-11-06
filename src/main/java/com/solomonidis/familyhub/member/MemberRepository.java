package com.solomonidis.familyhub.member;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Member> findAllByOrderByActiveDescNameAsc();

    List<Member> findAllByActiveTrueOrderByNameAsc();
}
