package com.ehms.dto;

import com.ehms.model.InvoiceStatus;
import com.ehms.model.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class InvoiceLine {
    private Long id;

    @NotNull(message = "Invoice date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate invoiceDate = LocalDate.now();

    @NotBlank(message = "Invoice description is required")
    @Size(max = 255)
    private String description;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0", message = "Unit price cannot be negative")
    private BigDecimal unitPrice;

    @NotNull @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    @DecimalMin("0") @DecimalMax("100")
    private BigDecimal vatRate = BigDecimal.ZERO;

    private PaymentMethod paymentMethod = PaymentMethod.CASH;
    private InvoiceStatus status = InvoiceStatus.UNPAID;
}
