package com.ehms.controller;

import com.ehms.dto.FeedbackForm;
import com.ehms.model.Feedback;
import com.ehms.repository.*;
import com.ehms.web.Qs;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Patients have no account: the secretary records the feedback they give at the desk. */
@Controller
@RequestMapping("/secretary/feedback")
public class SecretaryFeedbackController {

    private final FeedbackRepository feedbacks;
    private final PatientRepository patients;
    private final DoctorRepository doctors;

    public SecretaryFeedbackController(FeedbackRepository feedbacks, PatientRepository patients, DoctorRepository doctors) {
        this.feedbacks = feedbacks;
        this.patients = patients;
        this.doctors = doctors;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", feedbacks.search("", Pageable.unpaged()).getContent());
        return "secretary/feedback";
    }

    private String formView(Model model, FeedbackForm form) {
        model.addAttribute("form", form);
        model.addAttribute("patients", patients.findAllByOrderByLastNameAscFirstNameAsc());
        model.addAttribute("doctors", doctors.findAll(Sort.by("lastName")));
        return "secretary/feedback-form";
    }

    @GetMapping("/new")
    public String create(Model model) { return formView(model, new FeedbackForm()); }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Feedback fb = feedbacks.findById(id).orElse(null);
        if (fb == null) { ra.addFlashAttribute("error", "Feedback not found."); return "redirect:/secretary/feedback"; }
        FeedbackForm f = new FeedbackForm();
        f.setId(fb.getId());
        f.setPatientId(fb.getPatient().getId());
        f.setDoctorId(fb.getDoctor() == null ? null : fb.getDoctor().getId());
        f.setRating(fb.getRating());
        f.setComment(fb.getComment());
        return formView(model, f);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") FeedbackForm f, BindingResult result,
                       Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return formView(model, f);
        Feedback fb = f.getId() == null ? new Feedback() : feedbacks.findById(f.getId()).orElseThrow();
        fb.setPatient(patients.findById(f.getPatientId()).orElseThrow());
        fb.setDoctor(f.getDoctorId() == null ? null : doctors.findById(f.getDoctorId()).orElse(null));
        fb.setRating(f.getRating());
        fb.setComment(f.getComment() == null || f.getComment().isBlank() ? null : f.getComment().trim());
        feedbacks.save(fb);
        ra.addFlashAttribute("success", "Feedback saved.");
        return "redirect:/secretary/feedback";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        feedbacks.deleteById(id);
        ra.addFlashAttribute("success", "Feedback deleted.");
        return "redirect:/secretary/feedback";
    }
}
