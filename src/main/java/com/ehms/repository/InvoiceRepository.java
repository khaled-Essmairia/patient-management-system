package com.ehms.repository;

import com.ehms.model.Invoice;
import com.ehms.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("select i from Invoice i where (:status is null or i.status = :status) "
            + "and (:q = '' or lower(concat(i.patient.firstName, ' ', i.patient.lastName)) like lower(concat('%', :q, '%')) "
            + "     or lower(concat(i.patient.lastName, ' ', i.patient.firstName)) like lower(concat('%', :q, '%')) "
            + "     or lower(i.description) like lower(concat('%', :q, '%'))) "
            + "order by i.invoiceDate desc, i.id desc")
    Page<Invoice> search(@Param("status") InvoiceStatus status, @Param("q") String q, Pageable pageable);

    List<Invoice> findByPatientIdOrderByInvoiceDateDescIdDesc(Long patientId);
    List<Invoice> findByStatus(InvoiceStatus status);
    List<Invoice> findByStatusAndInvoiceDateBetween(InvoiceStatus status, LocalDate from, LocalDate to);
    void deleteByPatientId(Long patientId);
}
