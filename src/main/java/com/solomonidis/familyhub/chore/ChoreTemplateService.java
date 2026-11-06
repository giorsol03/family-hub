package com.solomonidis.familyhub.chore;

import com.solomonidis.familyhub.chore.ChoreDtos.ChoreTemplateRequest;
import com.solomonidis.familyhub.chore.ChoreDtos.ChoreTemplateResponse;
import com.solomonidis.familyhub.common.BusinessRuleException;
import com.solomonidis.familyhub.common.NotFoundException;
import com.solomonidis.familyhub.member.Member;
import com.solomonidis.familyhub.member.MemberRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chore templates ("defaults"). Deactivating hides a template and stops it generating new chores. */
@Service
@Transactional
public class ChoreTemplateService {

    private final ChoreTemplateRepository repository;
    private final MemberRepository memberRepository;

    public ChoreTemplateService(ChoreTemplateRepository repository, MemberRepository memberRepository) {
        this.repository = repository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<ChoreTemplateResponse> list(boolean includeInactive) {
        List<ChoreTemplate> templates = includeInactive
            ? repository.findAllByOrderByNameAsc()
            : repository.findByActiveTrueOrderByNameAsc();
        return templates.stream().map(ChoreTemplateResponse::of).toList();
    }

    public ChoreTemplateResponse create(ChoreTemplateRequest request) {
        ChoreTemplate template = new ChoreTemplate(request.name().trim());
        apply(template, request);
        return ChoreTemplateResponse.of(repository.save(template));
    }

    public ChoreTemplateResponse update(Long id, ChoreTemplateRequest request) {
        ChoreTemplate template = get(id);
        template.setName(request.name().trim());
        apply(template, request);
        return ChoreTemplateResponse.of(template);
    }

    public ChoreTemplateResponse setActive(Long id, boolean active) {
        ChoreTemplate template = get(id);
        template.setActive(active);
        return ChoreTemplateResponse.of(template);
    }

    private void apply(ChoreTemplate template, ChoreTemplateRequest request) {
        if (request.recurrence() == Recurrence.EVERY_N_DAYS && request.recurrenceInterval() == null) {
            throw new BusinessRuleException("recurrenceInterval", "How many days between repeats?");
        }
        template.setDescription(ChoreService.blankToNull(request.description()));
        template.setDefaultAssignee(findMember(request.defaultAssigneeId()));
        template.setEstimatedMinutes(request.estimatedMinutes());
        template.setRecurrence(request.recurrence(), request.recurrenceInterval());
    }

    private Member findMember(Long id) {
        if (id == null) {
            return null;
        }
        return memberRepository.findById(id)
            .orElseThrow(() -> new BusinessRuleException("defaultAssigneeId", "Unknown member"));
    }

    private ChoreTemplate get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Chore template " + id + " not found"));
    }
}
