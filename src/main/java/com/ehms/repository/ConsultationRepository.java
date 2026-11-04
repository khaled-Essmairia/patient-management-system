package com.ehms.repository;

import com.ehms.model.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    @Query("select c from Consultation c where :q = '' "
            + "or lower(concat(c.patient.firstName, ' ', c.patient.lastName)) like lower(concat('%', :q, '%')) "
            + "or lower(concat(c.patient.lastName, ' ', c.patient.firstName)) like lower(concat('%', :q, '%')) "
            + "or lower(c.reason) like lower(concat('%', :q, '%')) "
            + "or lower(c.diagnosis) like lower(concat('%', :q, '%')) "
            + "order by c.consultationDate desc, c.id desc")
    Page<Consultation> search(@Param("q") String q, Pageable pageable);

    List<Consultation> findByPatientIdOrderByConsultationDateDescIdDesc(Long patientId);
    List<Consultation> findTop5ByPatientIdOrderByConsultationDateDescIdDesc(Long patientId);
    boolean existsByDoctorId(Long doctorId);
    void deleteByPatientId(Long patientId);
}
