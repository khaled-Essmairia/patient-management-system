package com.ehms.repository;

import com.ehms.model.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("select r from Report r where (:doctorId is null or r.doctor.id = :doctorId) "
            + "and (:q = '' or lower(concat(r.patient.firstName, ' ', r.patient.lastName)) like lower(concat('%', :q, '%')) "
            + "     or lower(concat(r.patient.lastName, ' ', r.patient.firstName)) like lower(concat('%', :q, '%')) "
            + "     or lower(r.diagnosis) like lower(concat('%', :q, '%'))) "
            + "order by r.createdAt desc")
    Page<Report> search(@Param("doctorId") Long doctorId, @Param("q") String q, Pageable pageable);

    Optional<Report> findByAppointmentId(Long appointmentId);
    List<Report> findTop5ByPatientIdOrderByCreatedAtDesc(Long patientId);
    long countByDoctorId(Long doctorId);
    boolean existsByDoctorId(Long doctorId);
    void deleteByPatientId(Long patientId);
}
