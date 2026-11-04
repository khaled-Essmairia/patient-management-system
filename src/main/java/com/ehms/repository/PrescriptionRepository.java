package com.ehms.repository;

import com.ehms.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findAllByOrderByPrescriptionDateDescIdDesc();
    List<Prescription> findByPatientIdOrderByPrescriptionDateDescIdDesc(Long patientId);
    void deleteByPatientId(Long patientId);
}
