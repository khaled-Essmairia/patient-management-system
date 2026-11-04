package com.ehms.controller;

import com.ehms.model.Invoice;
import com.ehms.model.InvoiceStatus;
import com.ehms.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/secretary")
public class SecretaryDashboardController {

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final InvoiceRepository invoices;
    private final FeedbackRepository feedbacks;

    public SecretaryDashboardController(PatientRepository patients, DoctorRepository doctors,
                                        AppointmentRepository appointments, InvoiceRepository invoices,
                                        FeedbackRepository feedbacks) {
        this.patients = patients;
        this.doctors = doctors;
        this.appointments = appointments;
        this.invoices = invoices;
        this.feedbacks = feedbacks;
    }

    private static BigDecimal sum(List<Invoice> list) {
        return list.stream().map(Invoice::getTotalTtc).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping
    public String dashboard(Model model) {
        LocalDate today = LocalDate.now();
        var todays = appointments.findByAppointmentDateTimeBetweenOrderByAppointmentDateTime(
                today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        model.addAttribute("today", today);
        model.addAttribute("patientCount", patients.count());
        model.addAttribute("doctorCount", doctors.count());
        model.addAttribute("todayAppointments", todays);
        model.addAttribute("unpaidTotal", sum(invoices.findByStatus(InvoiceStatus.UNPAID)));
        model.addAttribute("monthRevenue", sum(invoices.findByStatusAndInvoiceDateBetween(
                InvoiceStatus.PAID, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()))));
        Double avg = feedbacks.averageRating();
        model.addAttribute("avgRating", avg == null ? "-" : String.format("%.1f / 5", avg));
        model.addAttribute("recentPatients", patients.findTop5ByOrderByCreatedAtDesc());
        return "secretary/dashboard";
    }
}
