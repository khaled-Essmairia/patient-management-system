package com.ehms.controller;

import com.ehms.dto.AppointmentForm;
import com.ehms.model.*;
import com.ehms.repository.*;
import com.ehms.service.LineMapper;
import com.ehms.sms.ReminderService;
import com.ehms.web.Qs;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/secretary/appointments")
public class SecretaryAppointmentController {

    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final ReportRepository reports;
    private final LineMapper mapper;
    private final ReminderService reminders;

    public SecretaryAppointmentController(AppointmentRepository appointments, PatientRepository patients,
                                          DoctorRepository doctors, ReportRepository reports, LineMapper mapper,
                                          ReminderService reminders) {
        this.reminders = reminders;
        this.appointments = appointments;
        this.patients = patients;
        this.doctors = doctors;
        this.reports = reports;
        this.mapper = mapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) AppointmentStatus status, Model model) {
        Page<Appointment> result = appointments.search(null, status, null, null, "",
                Pageable.unpaged(Sort.by(Sort.Direction.DESC, "appointmentDateTime")));
        model.addAttribute("rows", result.getContent());
        model.addAttribute("status", status);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("smsReady", reminders.isReady());
        model.addAttribute("smsHours", reminders.getHoursBefore());
        return "secretary/appointments";
    }

    private String formView(Model model, AppointmentForm form) {
        model.addAttribute("form", form);
        model.addAttribute("patients", patients.findAllByOrderByLastNameAscFirstNameAsc());
        model.addAttribute("doctors", doctors.findAll(Sort.by("lastName")));
        model.addAttribute("statuses", AppointmentStatus.values());
        return "secretary/appointment-form";
    }

    @GetMapping("/new")
    public String create(@RequestParam(required = false) Long patientId, Model model) {
        AppointmentForm f = new AppointmentForm();
        f.setPatientId(patientId);
        return formView(model, f);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Appointment a = appointments.findById(id).orElse(null);
        if (a == null) { ra.addFlashAttribute("error", "Appointment not found."); return "redirect:/secretary/appointments"; }
        AppointmentForm f = mapper.fill(new AppointmentForm(), a);
        f.setPatientId(a.getPatient().getId());
        return formView(model, f);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") AppointmentForm f, BindingResult result,
                       Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return formView(model, f);
        Appointment a = f.getId() == null ? new Appointment() : appointments.findById(f.getId()).orElseThrow();
        a.setPatient(patients.findById(f.getPatientId()).orElseThrow());
        mapper.apply(a, f);
        appointments.save(a);
        ra.addFlashAttribute("success", "Appointment saved.");
        return "redirect:/secretary/appointments";
    }

    @PostMapping("/{id}/remind")
    public String remind(@PathVariable Long id, RedirectAttributes ra) {
        ReminderService.Result r = reminders.sendNow(id);
        ra.addFlashAttribute(r.ok() ? "success" : "error", r.message());
        return "redirect:/secretary/appointments";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        reports.findByAppointmentId(id).ifPresent(reports::delete);
        appointments.deleteById(id);
        ra.addFlashAttribute("success", "Appointment deleted.");
        return "redirect:/secretary/appointments";
    }
}
