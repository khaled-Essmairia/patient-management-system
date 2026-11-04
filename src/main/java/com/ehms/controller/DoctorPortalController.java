package com.ehms.controller;

import com.ehms.dto.ReportForm;
import com.ehms.model.*;
import com.ehms.repository.*;
import com.ehms.web.Qs;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Everything a doctor can see is filtered on his own id. */
@Controller
@RequestMapping("/doctor")
public class DoctorPortalController {

    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final ReportRepository reports;
    private final ConsultationRepository consultations;

    public DoctorPortalController(DoctorRepository doctors, AppointmentRepository appointments,
                                  ReportRepository reports, ConsultationRepository consultations) {
        this.doctors = doctors;
        this.appointments = appointments;
        this.reports = reports;
        this.consultations = consultations;
    }

    private Doctor current(Authentication auth) {
        return doctors.findByUserUsername(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String dashboard(Authentication auth, Model model) {
        Doctor d = current(auth);
        LocalDate today = LocalDate.now();
        model.addAttribute("doctor", d);
        model.addAttribute("todayAppointments", appointments.findByDoctorIdAndAppointmentDateTimeBetweenOrderByAppointmentDateTime(
                d.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay()));
        model.addAttribute("upcomingCount", appointments.countByDoctorIdAndAppointmentDateTimeGreaterThanEqualAndStatus(
                d.getId(), LocalDateTime.now(), AppointmentStatus.SCHEDULED));
        model.addAttribute("completedCount", appointments.countByDoctorIdAndStatus(d.getId(), AppointmentStatus.COMPLETED));
        model.addAttribute("reportCount", reports.countByDoctorId(d.getId()));
        return "doctor/dashboard";
    }

    @GetMapping("/appointments")
    public String myAppointments(@RequestParam(defaultValue = "upcoming") String when,
                                 @RequestParam(defaultValue = "") String q,
                                 @RequestParam(required = false) AppointmentStatus status,
                                 @RequestParam(defaultValue = "0") int page,
                                 Authentication auth, Model model) {
        Doctor d = current(auth);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime from = null, to = null;
        Sort.Direction dir = Sort.Direction.DESC;
        switch (when) {
            case "today" -> { from = start; to = start.plusDays(1); dir = Sort.Direction.ASC; }
            case "past" -> to = start;
            case "all" -> { }
            default -> { when = "upcoming"; from = start; dir = Sort.Direction.ASC; }
        }
        Page<Appointment> result = appointments.search(d.getId(), status, from, to, "",
                Pageable.unpaged(Sort.by(dir, "appointmentDateTime")));
        model.addAttribute("rows", result.getContent());
        model.addAttribute("when", when);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("statuses", AppointmentStatus.values());
        model.addAttribute("qs", Qs.of("when", when, "q", q, "status", status == null ? null : status.name()));
        model.addAttribute("reportedIds", result.getContent().stream()
                .filter(a -> reports.findByAppointmentId(a.getId()).isPresent()).map(Appointment::getId).toList());
        return "doctor/appointments";
    }

    private Appointment owned(Long id, Authentication auth) {
        Doctor d = current(auth);
        return appointments.findById(id)
                .filter(a -> a.getDoctor().getId().equals(d.getId()))
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .orElse(null);
    }

    private String reportView(Model model, Appointment a, ReportForm form) {
        model.addAttribute("appointment", a);
        model.addAttribute("form", form);
        model.addAttribute("history", consultations.findTop5ByPatientIdOrderByConsultationDateDescIdDesc(a.getPatient().getId()));
        model.addAttribute("pastReports", reports.findTop5ByPatientIdOrderByCreatedAtDesc(a.getPatient().getId()).stream()
                .filter(r -> !r.getAppointment().getId().equals(a.getId())).toList());
        return "doctor/report-form";
    }

    @GetMapping("/appointments/{id}/report")
    public String reportForm(@PathVariable Long id, Authentication auth, Model model, RedirectAttributes ra) {
        Appointment a = owned(id, auth);
        if (a == null) { ra.addFlashAttribute("error", "Appointment not found."); return "redirect:/doctor/appointments"; }
        ReportForm f = new ReportForm();
        reports.findByAppointmentId(id).ifPresent(r -> {
            f.setDiagnosis(r.getDiagnosis());
            f.setMedicinePrescribed(r.getMedicinePrescribed());
            f.setDoctorComment(r.getDoctorComment());
        });
        return reportView(model, a, f);
    }

    @PostMapping("/appointments/{id}/report")
    @Transactional
    public String saveReport(@PathVariable Long id, @Valid @ModelAttribute("form") ReportForm f, BindingResult result,
                             Authentication auth, Model model, RedirectAttributes ra) {
        Appointment a = owned(id, auth);
        if (a == null) { ra.addFlashAttribute("error", "Appointment not found."); return "redirect:/doctor/appointments"; }
        if (result.hasErrors()) return reportView(model, a, f);
        Report r = reports.findByAppointmentId(id).orElseGet(() -> {
            Report n = new Report();
            n.setAppointment(a);
            n.setPatient(a.getPatient());
            n.setDoctor(a.getDoctor());
            return n;
        });
        r.setDiagnosis(f.getDiagnosis());
        r.setMedicinePrescribed(f.getMedicinePrescribed());
        r.setDoctorComment(f.getDoctorComment());
        reports.save(r);
        a.setStatus(AppointmentStatus.COMPLETED);
        appointments.save(a);
        ra.addFlashAttribute("success", "Report saved. The appointment is marked as completed.");
        return "redirect:/doctor/appointments?when=all";
    }

    @GetMapping("/reports")
    public String myReports(Authentication auth, Model model) {
        model.addAttribute("rows", reports.search(current(auth).getId(), "", Pageable.unpaged()).getContent());
        return "doctor/reports";
    }

    @GetMapping("/profile")
    public String profile(Authentication auth, Model model) {
        model.addAttribute("doctor", current(auth));
        return "doctor/profile";
    }
}
