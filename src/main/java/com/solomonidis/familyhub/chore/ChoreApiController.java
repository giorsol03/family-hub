package com.solomonidis.familyhub.chore;

import com.solomonidis.familyhub.chore.ChoreDtos.ChoreListResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreRequest;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreTemplateRequest;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreTemplateResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.FinishResponse;
import com.solomonidis.familyhub.chore.ChoreDtos.FromTemplateRequest;
import com.solomonidis.familyhub.chore.ChoreDtos.View;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/chores")
public class ChoreApiController {

    private final ChoreService choreService;
    private final ChoreTemplateService templateService;

    public ChoreApiController(ChoreService choreService, ChoreTemplateService templateService) {
        this.choreService = choreService;
        this.templateService = templateService;
    }

    /** {@code view} = TODAY (default), WEEK or OVERDUE; optional {@code assigneeId} filter. */
    @GetMapping
    public ChoreListResponse list(@RequestParam(defaultValue = "TODAY") View view,
                                  @RequestParam(required = false) Long assigneeId) {
        return choreService.list(view, assigneeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChoreResponse create(@Valid @RequestBody ChoreRequest request) {
        return choreService.create(request);
    }

    @PostMapping("/from-template/{templateId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ChoreResponse createFromTemplate(@PathVariable Long templateId,
                                            @RequestBody(required = false) FromTemplateRequest request) {
        return choreService.createFromTemplate(templateId,
            request == null ? null : request.scheduledDate(),
            request == null ? null : request.assigneeId());
    }

    @PutMapping("/{id}")
    public ChoreResponse update(@PathVariable Long id, @Valid @RequestBody ChoreRequest request) {
        return choreService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        choreService.delete(id);
    }

    @PostMapping("/{id}/complete")
    public FinishResponse complete(@PathVariable Long id) {
        return choreService.finish(id, ChoreStatus.DONE);
    }

    @PostMapping("/{id}/skip")
    public FinishResponse skip(@PathVariable Long id) {
        return choreService.finish(id, ChoreStatus.SKIPPED);
    }

    @PostMapping("/{id}/reopen")
    public ChoreResponse reopen(@PathVariable Long id) {
        return choreService.reopen(id);
    }

    // ---- templates ----

    @GetMapping("/templates")
    public List<ChoreTemplateResponse> templates(@RequestParam(defaultValue = "false") boolean includeInactive) {
        return templateService.list(includeInactive);
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public ChoreTemplateResponse createTemplate(@Valid @RequestBody ChoreTemplateRequest request) {
        return templateService.create(request);
    }

    @PutMapping("/templates/{id}")
    public ChoreTemplateResponse updateTemplate(@PathVariable Long id, @Valid @RequestBody ChoreTemplateRequest request) {
        return templateService.update(id, request);
    }

    @PostMapping("/templates/{id}/deactivate")
    public ChoreTemplateResponse deactivateTemplate(@PathVariable Long id) {
        return templateService.setActive(id, false);
    }

    @PostMapping("/templates/{id}/activate")
    public ChoreTemplateResponse activateTemplate(@PathVariable Long id) {
        return templateService.setActive(id, true);
    }
}
