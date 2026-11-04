package com.ehms.model;

public enum AppointmentStatus {
    SCHEDULED("Scheduled"), COMPLETED("Completed"), CANCELLED("Cancelled");
    private final String label;
    AppointmentStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
