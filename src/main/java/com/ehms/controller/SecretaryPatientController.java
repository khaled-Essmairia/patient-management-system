package com.ehms.controller;

import com.ehms.dto.PatientForm;
import com.ehms.model.*;
import com.ehms.repository.*;
import com.ehms.service.PatientService;
import com.ehms.web.Qs;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/secretary/patients")
public class SecretaryPatientController {

    private final PatientService service;
    private final PatientRepository patients;
    private final DoctorRepository doctors;

    public SecretaryPatientController(PatientService service, PatientRepository patients, DoctorRepository doctors) {
        this.service = service;
        this.patients = patients;
        this.doctors = doctors;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", patients.findAll(Sort.by("lastName", "firstName")));
        return "secretary/patients";
    }

    private String formView(Model model, PatientForm form) {
        model.addAttribute("form", form);
        model.addAttribute("doctors", doctors.findAll(Sort.by("lastName", "firstName")));
        model.addAttribute("appointmentStatuses", AppointmentStatus.values());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("invoiceStatuses", InvoiceStatus.values());
        Patient p = form.getId() == null ? null : patients.findById(form.getId()).orElse(null);
        model.addAttribute("ref", p == null ? null : p.getRef());
        return "secretary/patient-form";
    }

    @GetMapping("/new")
    public String create(Model model) {
        return formView(model, new PatientForm());
    }

    @GetMapping("/{id}")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        var form = service.loadForm(id);
        if (form.isEmpty()) {
            ra.addFlashAttribute("error", "Patient not found.");
            return "redirect:/secretary/patients";
        }
        return formView(model, form.get());
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") PatientForm form, BindingResult result,
                       Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return formView(model, form);
        Patient p = service.save(form);
        ra.addFlashAttribute("success", "Patient file saved.");
        return "redirect:/secretary/patients/" + p.getId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        service.delete(id);
        ra.addFlashAttribute("success", "Patient and all linked records deleted.");
        return "redirect:/secretary/patients";
    }
}
