package com.solomonidis.familyhub.calendar;

import com.solomonidis.familyhub.calendar.CalendarDtos.CalendarItem;
import com.solomonidis.familyhub.calendar.CalendarDtos.CalendarResponse;
import com.solomonidis.familyhub.calendar.CalendarDtos.EventRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarApiController {

    private final CalendarService service;

    public CalendarApiController(CalendarService service) {
        this.service = service;
    }

    /** Events and meal-plan entries between two dates (inclusive), optionally for one member. */
    @GetMapping
    public CalendarResponse items(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  @RequestParam(required = false) Long memberId) {
        return service.items(from, to, memberId);
    }

    @GetMapping("/events/{id}")
    public CalendarItem get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public CalendarItem create(@Valid @RequestBody EventRequest request) {
        return service.create(request);
    }

    @PutMapping("/events/{id}")
    public CalendarItem update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
