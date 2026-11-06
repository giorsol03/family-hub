package com.solomonidis.familyhub.calendar;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    /** Events overlapping [from, toExclusive). */
    @Query("""
        select e from CalendarEvent e
        where e.startAt < :toExclusive and e.endAt >= :from
        order by e.startAt, e.title""")
    List<CalendarEvent> findOverlapping(@Param("from") LocalDateTime from,
                                        @Param("toExclusive") LocalDateTime toExclusive);
}
