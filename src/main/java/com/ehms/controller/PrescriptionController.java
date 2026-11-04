package com.ehms.controller;

import com.ehms.dto.PrescriptionForm;
import com.ehms.model.Consultation;
import com.ehms.model.Prescription;
import com.ehms.repository.ConsultationRepository;
import com.ehms.repository.DoctorRepository;
import com.ehms.repository.PatientRepository;
import com.ehms.repository.PrescriptionRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/** Prescriptions (ordonnances). Open to the secretary and to the doctor. */
@Controller
@RequestMapping("/prescriptions")
public class PrescriptionController {

    private final PrescriptionRepository prescriptions;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final ConsultationRepository consultations;

    public PrescriptionController(PrescriptionRepository prescriptions, PatientRepository patients,
                                  DoctorRepository doctors, ConsultationRepository consultations) {
        this.prescriptions = prescriptions;
        this.patients = patients;
        this.doctors = doctors;
        this.consultations = consultations;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", prescriptions.findAllByOrderByPrescriptionDateDescIdDesc());
        return "prescriptions/list";
    }

    private String formView(Model model, PrescriptionForm form) {
        model.addAttribute("form", form);
        model.addAttribute("patients", patients.findAllByOrderByLastNameAscFirstNameAsc());
        return "prescriptions/form";
    }

    /** {@code consultationId} pre-fills the patient and the treatment written in that consultation. */
    @GetMapping("/new")
    public String create(@RequestParam(required = false) Long patientId,
                         @RequestParam(required = false) Long consultationId, Model model) {
        PrescriptionForm f = new PrescriptionForm();
        f.setPrescriptionDate(LocalDate.now());
        f.setPatientId(patientId);
        if (consultationId != null) {
            Consultation c = consultations.findById(consultationId).orElse(null);
            if (c != null) {
                f.setPatientId(c.getPatient().getId());
                f.setContent(c.getTreatment());
            }
        }
        return formView(model, f);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Prescription p = prescriptions.findById(id).orElse(null);
        if (p == null) { ra.addFlashAttribute("error", "Prescription not found."); return "redirect:/prescriptions"; }
        PrescriptionForm f = new PrescriptionForm();
        f.setId(p.getId());
        f.setPatientId(p.getPatient().getId());
        f.setPrescriptionDate(p.getPrescriptionDate());
        f.setContent(p.getContent());
        f.setAdvice(p.getAdvice());
        return formView(model, f);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") PrescriptionForm f, BindingResult result,
                       @RequestParam(required = false) String print, Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return formView(model, f);
        Prescription p = f.getId() == null ? new Prescription() : prescriptions.findById(f.getId()).orElseThrow();
        p.setPatient(patients.findById(f.getPatientId()).orElseThrow());
        // one doctor per cabinet
        p.setDoctor(doctors.findAll().stream().findFirst().orElse(null));
        p.setPrescriptionDate(f.getPrescriptionDate());
        p.setContent(f.getContent().trim());
        p.setAdvice(f.getAdvice() == null || f.getAdvice().isBlank() ? null : f.getAdvice().trim());
        p = prescriptions.save(p);
        ra.addFlashAttribute("success", "Prescription saved.");
        return print != null ? "redirect:/prescriptions/" + p.getId() + "/print" : "redirect:/prescriptions";
    }

    @GetMapping("/{id}/print")
    public String print(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Prescription p = prescriptions.findById(id).orElse(null);
        if (p == null) { ra.addFlashAttribute("error", "Prescription not found."); return "redirect:/prescriptions"; }
        model.addAttribute("p", p);
        model.addAttribute("doctor", p.getDoctor() != null ? p.getDoctor() : doctors.findAll().stream().findFirst().orElse(null));
        return "print/prescription";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        prescriptions.deleteById(id);
        ra.addFlashAttribute("success", "Prescription deleted.");
        return "redirect:/prescriptions";
    }
}
