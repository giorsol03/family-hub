package com.solomonidis.familyhub.chore;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChoreTemplateRepository extends JpaRepository<ChoreTemplate, Long> {

    @EntityGraph(attributePaths = "defaultAssignee")
    List<ChoreTemplate> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "defaultAssignee")
    List<ChoreTemplate> findByActiveTrueOrderByNameAsc();
}
