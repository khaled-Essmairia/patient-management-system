package com.ehms.repository;

import com.ehms.model.Appointment;
import com.ehms.model.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("select a from Appointment a where (:doctorId is null or a.doctor.id = :doctorId) "
            + "and (:status is null or a.status = :status) "
            + "and (:from is null or a.appointmentDateTime >= :from) "
            + "and (:to is null or a.appointmentDateTime < :to) "
            + "and (:q = '' or lower(concat(a.patient.firstName, ' ', a.patient.lastName)) like lower(concat('%', :q, '%')) "
            + "     or lower(concat(a.patient.lastName, ' ', a.patient.firstName)) like lower(concat('%', :q, '%')))")
    Page<Appointment> search(@Param("doctorId") Long doctorId, @Param("status") AppointmentStatus status,
                             @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                             @Param("q") String q, Pageable pageable);

    @Query("select a from Appointment a where a.status = :status and a.reminderSentAt is null "
            + "and coalesce(a.reminderAttempts, 0) < :maxAttempts "
            + "and a.appointmentDateTime > :from and a.appointmentDateTime <= :to "
            + "order by a.appointmentDateTime")
    List<Appointment> findDueReminders(@Param("status") AppointmentStatus status, @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to, @Param("maxAttempts") int maxAttempts);

    List<Appointment> findByPatientIdOrderByAppointmentDateTimeDesc(Long patientId);
    List<Appointment> findByAppointmentDateTimeBetweenOrderByAppointmentDateTime(LocalDateTime from, LocalDateTime to);
    List<Appointment> findByDoctorIdAndAppointmentDateTimeBetweenOrderByAppointmentDateTime(Long doctorId, LocalDateTime from, LocalDateTime to);

    long countByDoctorIdAndAppointmentDateTimeGreaterThanEqualAndStatus(Long doctorId, LocalDateTime from, AppointmentStatus status);
    long countByDoctorIdAndStatus(Long doctorId, AppointmentStatus status);
    boolean existsByDoctorId(Long doctorId);

    void deleteByPatientId(Long patientId);
}
