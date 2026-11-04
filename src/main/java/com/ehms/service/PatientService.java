package com.ehms.service;

import com.ehms.dto.*;
import com.ehms.model.*;
import com.ehms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Saves a whole patient file in one transaction: identity data plus the consultation,
 * appointment and invoice lines. Lines missing from the submitted form are deleted.
 */
@Service
public class PatientService {

    private final PatientRepository patients;
    private final DoctorRepository doctors;
    private final ConsultationRepository consultations;
    private final AppointmentRepository appointments;
    private final InvoiceRepository invoices;
    private final ReportRepository reports;
    private final FeedbackRepository feedbacks;
    private final PrescriptionRepository prescriptions;
    private final LineMapper mapper;

    public PatientService(PatientRepository patients, DoctorRepository doctors, ConsultationRepository consultations,
                          AppointmentRepository appointments, InvoiceRepository invoices, ReportRepository reports,
                          FeedbackRepository feedbacks, PrescriptionRepository prescriptions, LineMapper mapper) {
        this.patients = patients;
        this.doctors = doctors;
        this.consultations = consultations;
        this.appointments = appointments;
        this.invoices = invoices;
        this.reports = reports;
        this.feedbacks = feedbacks;
        this.prescriptions = prescriptions;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Optional<PatientForm> loadForm(Long id) {
        return patients.findById(id).map(p -> {
            PatientForm f = new PatientForm();
            f.setId(p.getId());
            f.setFirstName(p.getFirstName());
            f.setLastName(p.getLastName());
            f.setGender(p.getGender());
            f.setDateOfBirth(p.getDateOfBirth());
            f.setContactNumber(p.getContactNumber());
            f.setNotes(p.getNotes());
            f.setConsultations(consultations.findByPatientIdOrderByConsultationDateDescIdDesc(id).stream()
                    .map(c -> mapper.fill(new ConsultationLine(), c)).collect(Collectors.toList()));
            f.setAppointments(appointments.findByPatientIdOrderByAppointmentDateTimeDesc(id).stream()
                    .map(a -> mapper.fill(new AppointmentLine(), a)).collect(Collectors.toList()));
            f.setInvoices(invoices.findByPatientIdOrderByInvoiceDateDescIdDesc(id).stream()
                    .map(i -> mapper.fill(new InvoiceLine(), i)).collect(Collectors.toList()));
            return f;
        });
    }

    @Transactional
    public Patient save(PatientForm f) {
        Patient p = f.getId() == null ? new Patient() : patients.findById(f.getId()).orElseThrow();
        p.setFirstName(f.getFirstName().trim());
        p.setLastName(f.getLastName().trim());
        p.setGender(f.getGender());
        p.setDateOfBirth(f.getDateOfBirth());
        p.setContactNumber(f.getContactNumber().trim());
        p.setNotes(blankToNull(f.getNotes()));
        final Patient saved = patients.save(p);

        // consultations
        Map<Long, Consultation> oldC = byId(consultations.findByPatientIdOrderByConsultationDateDescIdDesc(saved.getId()), Consultation::getId);
        Set<Long> keepC = new HashSet<>();
        for (ConsultationLine l : f.getConsultations()) {
            Consultation c = l.getId() == null ? null : oldC.get(l.getId());
            if (c == null) { c = new Consultation(); c.setPatient(saved); } else keepC.add(c.getId());
            mapper.apply(c, l);
            consultations.save(c);
        }
        oldC.values().stream().filter(c -> !keepC.contains(c.getId())).forEach(consultations::delete);

        // appointments (a deleted appointment also loses its report)
        Map<Long, Appointment> oldA = byId(appointments.findByPatientIdOrderByAppointmentDateTimeDesc(saved.getId()), Appointment::getId);
        Set<Long> keepA = new HashSet<>();
        for (AppointmentLine l : f.getAppointments()) {
            Appointment a = l.getId() == null ? null : oldA.get(l.getId());
            if (a == null) { a = new Appointment(); a.setPatient(saved); } else keepA.add(a.getId());
            mapper.apply(a, l);
            appointments.save(a);
        }
        oldA.values().stream().filter(a -> !keepA.contains(a.getId())).forEach(a -> {
            reports.findByAppointmentId(a.getId()).ifPresent(reports::delete);
            appointments.delete(a);
        });

        // invoices
        Map<Long, Invoice> oldI = byId(invoices.findByPatientIdOrderByInvoiceDateDescIdDesc(saved.getId()), Invoice::getId);
        Set<Long> keepI = new HashSet<>();
        for (InvoiceLine l : f.getInvoices()) {
            Invoice i = l.getId() == null ? null : oldI.get(l.getId());
            if (i == null) { i = new Invoice(); i.setPatient(saved); } else keepI.add(i.getId());
            mapper.apply(i, l);
            invoices.save(i);
        }
        oldI.values().stream().filter(i -> !keepI.contains(i.getId())).forEach(invoices::delete);
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        reports.deleteByPatientId(id);
        feedbacks.deleteByPatientId(id);
        prescriptions.deleteByPatientId(id);
        invoices.deleteByPatientId(id);
        consultations.deleteByPatientId(id);
        appointments.deleteByPatientId(id);
        patients.deleteById(id);
    }

    private static <T> Map<Long, T> byId(List<T> list, Function<T, Long> id) {
        return list.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
