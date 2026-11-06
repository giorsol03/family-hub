package com.solomonidis.familyhub.goal;

import com.solomonidis.familyhub.goal.GoalDtos.CheckRequest;
import com.solomonidis.familyhub.goal.GoalDtos.GoalRequest;
import com.solomonidis.familyhub.goal.GoalDtos.GoalResponse;
import com.solomonidis.familyhub.goal.GoalDtos.GoalRow;
import com.solomonidis.familyhub.goal.GoalDtos.MonthResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/goals")
public class GoalApiController {

    private final GoalService service;

    public GoalApiController(GoalService service) {
        this.service = service;
    }

    /** Monthly grid; {@code month} like "2026-11" (default: this month), optional {@code ownerId}. */
    @GetMapping("/month")
    public MonthResponse month(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                               @RequestParam(required = false) Long ownerId) {
        return service.month(month, ownerId);
    }

    @PutMapping("/{id}/checks/{date}")
    public GoalRow setCheck(@PathVariable Long id,
                            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @Valid @RequestBody CheckRequest request) {
        return service.setCheck(id, date, request.done());
    }

    @GetMapping
    public List<GoalResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(includeInactive);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@Valid @RequestBody GoalRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable Long id, @Valid @RequestBody GoalRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/deactivate")
    public GoalResponse deactivate(@PathVariable Long id) {
        return service.setActive(id, false);
    }

    @PostMapping("/{id}/activate")
    public GoalResponse activate(@PathVariable Long id) {
        return service.setActive(id, true);
    }
}
