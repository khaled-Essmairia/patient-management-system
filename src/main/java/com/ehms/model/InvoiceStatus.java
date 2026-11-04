package com.ehms.model;

public enum InvoiceStatus {
    UNPAID("Unpaid"), PAID("Paid");
    private final String label;
    InvoiceStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
