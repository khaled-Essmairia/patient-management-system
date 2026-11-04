package com.ehms.model;

public enum PaymentMethod {
    CASH("Cash"), CARD("Bank card"), CHEQUE("Cheque"), TRANSFER("Bank transfer"), INSURANCE("Insurance");
    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
