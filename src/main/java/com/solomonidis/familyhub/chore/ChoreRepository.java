package com.solomonidis.familyhub.chore;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChoreRepository extends JpaRepository<Chore, Long> {

    @EntityGraph(attributePaths = {"assignee", "completedBy", "template"})
    @Query("""
        select c from Chore c
        where c.scheduledDate between :from and :to
          and (:assigneeId is null or c.assignee.id = :assigneeId)
        order by c.scheduledDate, c.title""")
    List<Chore> findScheduledBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                     @Param("assigneeId") Long assigneeId);

    @EntityGraph(attributePaths = {"assignee", "completedBy", "template"})
    @Query("""
        select c from Chore c
        where c.status = com.solomonidis.familyhub.chore.ChoreStatus.TODO
          and c.scheduledDate < :today
          and (:assigneeId is null or c.assignee.id = :assigneeId)
        order by c.scheduledDate, c.title""")
    List<Chore> findOverdue(@Param("today") LocalDate today, @Param("assigneeId") Long assigneeId);

    boolean existsByTemplateAndStatusAndScheduledDate(ChoreTemplate template, ChoreStatus status, LocalDate date);
}
