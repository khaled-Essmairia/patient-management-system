package com.ehms.config;

import com.ehms.model.AppUser;
import com.ehms.model.Doctor;
import com.ehms.model.Role;
import com.ehms.repository.DoctorRepository;
import com.ehms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

/**
 * First start only: one secretary account (secretary / 123456) and the cabinet doctor, to rename from "Doctor profile"
 * (doctor / 123456). Change these passwords before real use.
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(UserRepository users, DoctorRepository doctors, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByUsername("secretary")) {
                users.save(new AppUser("Clinic Secretary", "secretary", encoder.encode("123456"), Role.SECRETARY));
            }
            if (doctors.count() == 0) {
                Object[][] d = {
                    {"Cabinet", "Doctor", "M", "", "MD", "General Practitioner", 50, ""}
                };
                String hash = encoder.encode("123456");
                int i = 1;
                for (Object[] r : d) {
                    AppUser u = users.save(new AppUser(r[0] + " " + r[1], "doctor", hash, Role.DOCTOR));
                    Doctor doc = new Doctor();
                    doc.setUser(u);
                    doc.setFirstName((String) r[0]);
                    doc.setLastName((String) r[1]);
                    doc.setGender((String) r[2]);
                    doc.setContactNumber((String) r[3]);
                    doc.setQualification((String) r[4]);
                    doc.setDoctorType((String) r[5]);
                    doc.setConsultationFee(BigDecimal.valueOf((Integer) r[6]));
                    doc.setEmail((String) r[7]);
                    doctors.save(doc);
                }
            }
        };
    }
}
