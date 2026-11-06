package com.solomonidis.familyhub.goal;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    @EntityGraph(attributePaths = "owner")
    List<Goal> findAllByOrderByOwnerNameAscNameAsc();
}
