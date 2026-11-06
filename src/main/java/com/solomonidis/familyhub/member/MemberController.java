package com.solomonidis.familyhub.member;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("members", memberService.findAll());
        return "member/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new MemberForm());
        return "member/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") MemberForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                Member member = memberService.create(form);
                redirect.addFlashAttribute("success", member.getName() + " added");
                return "redirect:/members";
            } catch (MemberException e) {
                reject(result, e);
            }
        }
        return "member/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("memberId", id);
        model.addAttribute("form", MemberForm.from(memberService.get(id)));
        return "member/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") MemberForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                Member member = memberService.update(id, form);
                redirect.addFlashAttribute("success", member.getName() + " updated");
                return "redirect:/members";
            } catch (MemberException e) {
                reject(result, e);
            }
        }
        model.addAttribute("memberId", id);
        return "member/form";
    }

    @PostMapping("/{id}/deactivate")
    public String deactivate(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal me,
                             RedirectAttributes redirect) {
        try {
            memberService.deactivate(id, me.id());
            redirect.addFlashAttribute("success", "Member deactivated");
        } catch (MemberException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/members";
    }

    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirect) {
        memberService.activate(id);
        redirect.addFlashAttribute("success", "Member re-activated");
        return "redirect:/members";
    }

    private static void reject(BindingResult result, MemberException e) {
        if (e.getField() != null) {
            result.rejectValue(e.getField(), "member.invalid", e.getMessage());
        } else {
            result.reject("member.invalid", e.getMessage());
        }
    }
}
