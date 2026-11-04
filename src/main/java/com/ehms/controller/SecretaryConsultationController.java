package com.ehms.controller;

import com.ehms.dto.ConsultationForm;
import com.ehms.model.Consultation;
import com.ehms.repository.*;
import com.ehms.service.LineMapper;
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

@Controller
@RequestMapping("/secretary/consultations")
public class SecretaryConsultationController {

    private final ConsultationRepository consultations;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final LineMapper mapper;

    public SecretaryConsultationController(ConsultationRepository consultations, PatientRepository patients,
                                           DoctorRepository doctors, LineMapper mapper) {
        this.consultations = consultations;
        this.patients = patients;
        this.doctors = doctors;
        this.mapper = mapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", consultations.search("", Pageable.unpaged()).getContent());
        return "secretary/consultations";
    }

    private String formView(Model model, ConsultationForm form) {
        model.addAttribute("form", form);
        model.addAttribute("patients", patients.findAllByOrderByLastNameAscFirstNameAsc());
        model.addAttribute("doctors", doctors.findAll(Sort.by("lastName")));
        return "secretary/consultation-form";
    }

    @GetMapping("/new")
    public String create(@RequestParam(required = false) Long patientId, Model model) {
        ConsultationForm f = new ConsultationForm();
        f.setPatientId(patientId);
        f.setConsultationDate(java.time.LocalDate.now());
        return formView(model, f);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Consultation c = consultations.findById(id).orElse(null);
        if (c == null) { ra.addFlashAttribute("error", "Consultation not found."); return "redirect:/secretary/consultations"; }
        ConsultationForm f = mapper.fill(new ConsultationForm(), c);
        f.setPatientId(c.getPatient().getId());
        return formView(model, f);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") ConsultationForm f, BindingResult result,
                       Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return formView(model, f);
        Consultation c = f.getId() == null ? new Consultation() : consultations.findById(f.getId()).orElseThrow();
        c.setPatient(patients.findById(f.getPatientId()).orElseThrow());
        mapper.apply(c, f);
        consultations.save(c);
        ra.addFlashAttribute("success", "Consultation saved.");
        return "redirect:/secretary/consultations";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        consultations.deleteById(id);
        ra.addFlashAttribute("success", "Consultation deleted.");
        return "redirect:/secretary/consultations";
    }
}
