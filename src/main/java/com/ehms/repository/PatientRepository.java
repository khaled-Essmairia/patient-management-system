package com.ehms.repository;

import com.ehms.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    @Query("select p from Patient p where :q = '' "
            + "or lower(concat(p.firstName, ' ', p.lastName)) like lower(concat('%', :q, '%')) "
            + "or lower(concat(p.lastName, ' ', p.firstName)) like lower(concat('%', :q, '%')) "
            + "or p.contactNumber like concat('%', :q, '%')")
    Page<Patient> search(@Param("q") String q, Pageable pageable);

    List<Patient> findAllByOrderByLastNameAscFirstNameAsc();
    List<Patient> findTop5ByOrderByCreatedAtDesc();
    boolean existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(String firstName, String lastName, java.time.LocalDate dateOfBirth);
}
