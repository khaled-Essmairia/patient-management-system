package com.ehms.repository;

import com.ehms.model.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    @Query("select f from Feedback f where :q = '' "
            + "or lower(concat(f.patient.firstName, ' ', f.patient.lastName)) like lower(concat('%', :q, '%')) "
            + "or lower(f.comment) like lower(concat('%', :q, '%')) "
            + "order by f.createdAt desc")
    Page<Feedback> search(@Param("q") String q, Pageable pageable);

    @Query("select avg(f.rating) from Feedback f")
    Double averageRating();

    boolean existsByDoctorId(Long doctorId);
    void deleteByPatientId(Long patientId);
}
