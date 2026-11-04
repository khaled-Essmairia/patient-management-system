package com.ehms.service;

import com.ehms.dto.*;
import com.ehms.model.*;
import com.ehms.repository.DoctorRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Copies data between the form lines and the entities (shared by the patient file and the standalone forms). */
@Component
public class LineMapper {

    private final DoctorRepository doctors;

    public LineMapper(DoctorRepository doctors) { this.doctors = doctors; }

    /** One doctor per cabinet: every consultation and appointment belongs to him. */
    private Doctor doctor(Long ignored) { return doctors.findAll().stream().findFirst().orElse(null); }

    private static String clean(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    // ---- consultation
    public void apply(Consultation c, ConsultationLine l) {
        c.setConsultationDate(l.getConsultationDate());
        c.setDoctor(doctor(l.getDoctorId()));
        c.setReason(l.getReason().trim());
        c.setSymptoms(clean(l.getSymptoms()));
        c.setDiagnosis(clean(l.getDiagnosis()));
        c.setTreatment(clean(l.getTreatment()));
        c.setNotes(clean(l.getNotes()));
        c.setNextVisit(l.getNextVisit());
    }

    public <T extends ConsultationLine> T fill(T l, Consultation c) {
        l.setId(c.getId());
        l.setConsultationDate(c.getConsultationDate());
        l.setDoctorId(c.getDoctor() == null ? null : c.getDoctor().getId());
        l.setReason(c.getReason());
        l.setSymptoms(c.getSymptoms());
        l.setDiagnosis(c.getDiagnosis());
        l.setTreatment(c.getTreatment());
        l.setNotes(c.getNotes());
        l.setNextVisit(c.getNextVisit());
        return l;
    }

    // ---- appointment
    public void apply(Appointment a, AppointmentLine l) {
        Doctor d = doctor(l.getDoctorId());
        a.setDoctor(d);
        if (a.getAppointmentDateTime() != null && !a.getAppointmentDateTime().equals(l.getAppointmentDateTime())) {
            // the appointment moved: the patient must get a new reminder
            a.setReminderSentAt(null);
            a.setReminderAttempts(0);
        }
        a.setAppointmentDateTime(l.getAppointmentDateTime());
        a.setReason(clean(l.getReason()));
        a.setStatus(l.getStatus() == null ? AppointmentStatus.SCHEDULED : l.getStatus());
        BigDecimal fee = l.getFee();
        if (fee == null && d != null) fee = d.getConsultationFee();
        a.setFee(fee);
    }

    public <T extends AppointmentLine> T fill(T l, Appointment a) {
        l.setId(a.getId());
        l.setAppointmentDateTime(a.getAppointmentDateTime());
        l.setDoctorId(a.getDoctor().getId());
        l.setReason(a.getReason());
        l.setStatus(a.getStatus());
        l.setFee(a.getFee());
        return l;
    }

    // ---- invoice
    public void apply(Invoice i, InvoiceLine l) {
        i.setInvoiceDate(l.getInvoiceDate());
        i.setDescription(l.getDescription().trim());
        i.setUnitPrice(l.getUnitPrice());
        i.setQuantity(l.getQuantity());
        i.setVatRate(l.getVatRate() == null ? BigDecimal.ZERO : l.getVatRate());
        i.setPaymentMethod(l.getPaymentMethod() == null ? PaymentMethod.CASH : l.getPaymentMethod());
        i.setStatus(l.getStatus() == null ? InvoiceStatus.UNPAID : l.getStatus());
    }

    public <T extends InvoiceLine> T fill(T l, Invoice i) {
        l.setId(i.getId());
        l.setInvoiceDate(i.getInvoiceDate());
        l.setDescription(i.getDescription());
        l.setUnitPrice(i.getUnitPrice());
        l.setQuantity(i.getQuantity());
        l.setVatRate(i.getVatRate());
        l.setPaymentMethod(i.getPaymentMethod());
        l.setStatus(i.getStatus());
        return l;
    }
}
