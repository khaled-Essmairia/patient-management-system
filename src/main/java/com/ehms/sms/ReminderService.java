package com.ehms.sms;

import com.ehms.model.Appointment;
import com.ehms.model.AppointmentStatus;
import com.ehms.repository.AppointmentRepository;
import com.ehms.web.ClinicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Sends an SMS reminder to patients before their appointment (automatically, or on demand). */
@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    /** English text, translated by the screen that shows it. */
    public record Result(boolean ok, String message) { }

    private final AppointmentRepository appointments;
    private final SmsService sms;
    private final MessageSource messages;
    private final ClinicInfo clinic;
    private final int hoursBefore;
    private final int fromHour;
    private final int untilHour;
    private final Locale language;

    public ReminderService(AppointmentRepository appointments, SmsService sms, MessageSource messages, ClinicInfo clinic,
                           @Value("${sms.reminder-hours-before:24}") int hoursBefore,
                           @Value("${sms.send-from-hour:8}") int fromHour,
                           @Value("${sms.send-until-hour:20}") int untilHour,
                           @Value("${sms.language:fr}") String language) {
        this.appointments = appointments;
        this.sms = sms;
        this.messages = messages;
        this.clinic = clinic;
        this.hoursBefore = hoursBefore;
        this.fromHour = fromHour;
        this.untilHour = untilHour;
        this.language = Locale.forLanguageTag(language);
    }

    public boolean isReady() { return sms.isReady(); }
    public int getHoursBefore() { return hoursBefore; }

    /** Every 15 minutes: remind the patients whose appointment is coming up. */
    @Scheduled(initialDelay = 120_000, fixedDelay = 900_000)
    public void sendDueReminders() {
        if (!sms.isReady()) return;
        int hour = LocalDateTime.now().getHour();
        if (hour < fromHour || hour >= untilHour) return; // no SMS at night
        LocalDateTime now = LocalDateTime.now();
        List<Appointment> due = appointments.findDueReminders(AppointmentStatus.SCHEDULED, now,
                now.plusHours(hoursBefore), MAX_ATTEMPTS);
        for (Appointment a : due) {
            Result r = remind(a);
            if (!r.ok()) log.warn("Reminder for appointment {} not sent: {}", a.getId(), r.message());
        }
    }

    /** "Send reminder" button. */
    public Result sendNow(Long appointmentId) {
        Appointment a = appointments.findById(appointmentId).orElse(null);
        if (a == null) return new Result(false, "Appointment not found.");
        if (!sms.isReady()) return new Result(false, "SMS is not configured. Set the sms.* properties.");
        if (a.getStatus() != AppointmentStatus.SCHEDULED) return new Result(false, "Reminders are only sent for scheduled appointments.");
        return remind(a);
    }

    private Result remind(Appointment a) {
        String phone = a.getPatient().getContactNumber();
        if (sms.normalize(phone) == null) return new Result(false, "This patient has no valid phone number.");
        int attempts = a.getReminderAttempts() == null ? 0 : a.getReminderAttempts();
        try {
            sms.send(phone, text(a));
            a.setReminderSentAt(LocalDateTime.now());
            a.setReminderAttempts(attempts + 1);
            appointments.save(a);
            return new Result(true, "Reminder sent.");
        } catch (Exception e) {
            a.setReminderAttempts(attempts + 1);
            appointments.save(a);
            log.warn("SMS failed for appointment {}: {}", a.getId(), e.getMessage());
            return new Result(false, "The SMS could not be sent: " + e.getMessage());
        }
    }

    private String text(Appointment a) {
        Object[] args = {
                a.getPatient().getFullName(),
                a.getDoctor() == null ? "" : a.getDoctor().getDisplayName(),
                a.getAppointmentDateTime().format(DATE),
                a.getAppointmentDateTime().format(TIME),
                clinic.getName(),
                clinic.getPhone()
        };
        return messages.getMessage("sms.reminder", args, language);
    }
}
