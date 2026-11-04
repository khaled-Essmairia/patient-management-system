package com.ehms.web;

import com.ehms.model.Doctor;
import com.ehms.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Cabinet identity printed in the header of prescriptions, invoices and receipts. Used in templates as ${@clinic.name}. */
@Component("clinic")
public class ClinicInfo {

    private final DoctorRepository doctors;
    private final String name;
    private final String address;
    private final String phone;
    private final String footer;

    public ClinicInfo(DoctorRepository doctors,
                      @Value("${clinic.name:Clinic}") String name,
                      @Value("${clinic.address:}") String address,
                      @Value("${clinic.phone:}") String phone,
                      @Value("${clinic.footer:}") String footer) {
        this.doctors = doctors;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.footer = footer;
    }

    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public String getFooter() { return footer; }

    /** One doctor per cabinet. */
    public Doctor getDoctor() { return doctors.findAll().stream().findFirst().orElse(null); }
}
