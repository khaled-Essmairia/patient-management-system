package com.ehms.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Getter @Setter @NoArgsConstructor
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    private Patient patient;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    public String getReference() { return id == null ? "" : String.format("INV-%06d", id); }

    public BigDecimal getTotalHt() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(3, RoundingMode.HALF_UP);
    }
    public BigDecimal getVatAmount() {
        return getTotalHt().multiply(vatRate).divide(BigDecimal.valueOf(100), 3, RoundingMode.HALF_UP);
    }
    public BigDecimal getTotalTtc() { return getTotalHt().add(getVatAmount()); }
}
