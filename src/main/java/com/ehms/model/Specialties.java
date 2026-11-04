package com.ehms.model;

import java.util.List;

public final class Specialties {
    public static final List<String> ALL = List.of(
            "General Practitioner", "Dentist", "Ophthalmologist", "Cardiologist", "ENT",
            "Dermatologist", "Pediatrician", "Gynecologist", "Orthopedist", "Neurologist", "Other");
    public static final List<String> BLOOD_GROUPS = List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    private Specialties() { }
}
