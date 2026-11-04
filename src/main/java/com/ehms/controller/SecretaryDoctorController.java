package com.ehms.controller;

import com.ehms.dto.DoctorForm;
import com.ehms.model.*;
import com.ehms.repository.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/secretary/doctors")
public class SecretaryDoctorController {

    private final UserRepository users;
    private final DoctorRepository doctors;
    private final AppointmentRepository appointments;
    private final ConsultationRepository consultations;
    private final ReportRepository reports;
    private final FeedbackRepository feedbacks;
    private final PasswordEncoder encoder;

    public SecretaryDoctorController(UserRepository users, DoctorRepository doctors, AppointmentRepository appointments,
                                     ConsultationRepository consultations, ReportRepository reports,
                                     FeedbackRepository feedbacks, PasswordEncoder encoder) {
        this.users = users;
        this.doctors = doctors;
        this.appointments = appointments;
        this.consultations = consultations;
        this.reports = reports;
        this.feedbacks = feedbacks;
        this.encoder = encoder;
    }

    /** One doctor per cabinet: the list page simply opens that doctor's profile. */
    @GetMapping
    public String list(RedirectAttributes ra) {
        return doctors.findAll().stream().findFirst()
                .map(d -> "redirect:/secretary/doctors/" + d.getId() + "/edit")
                .orElse("redirect:/secretary");
    }

    private String form(Model model) {
        model.addAttribute("specialties", Specialties.ALL);
        return "secretary/doctor-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Doctor d = doctors.findById(id).orElse(null);
        if (d == null) { ra.addFlashAttribute("error", "Doctor not found."); return "redirect:/secretary"; }
        DoctorForm f = new DoctorForm();
        f.setId(d.getId());
        f.setUsername(d.getUser() == null ? "" : d.getUser().getUsername());
        f.setFirstName(d.getFirstName());
        f.setLastName(d.getLastName());
        f.setGender(d.getGender());
        f.setContactNumber(d.getContactNumber());
        f.setEmail(d.getEmail());
        f.setQualification(d.getQualification());
        f.setDoctorType(d.getDoctorType());
        f.setConsultationFee(d.getConsultationFee());
        model.addAttribute("form", f);
        return form(model);
    }

    @PostMapping
    @Transactional
    public String save(@Valid @ModelAttribute("form") DoctorForm f, BindingResult result, Model model, RedirectAttributes ra) {
        boolean creating = f.getId() == null;
        String pwd = f.getPassword() == null ? "" : f.getPassword();
        if (creating && pwd.isBlank()) result.rejectValue("password", "required", "Password is required");
        else if (!pwd.isBlank() && pwd.length() < 4) result.rejectValue("password", "short", "At least 4 characters");
        if (creating && users.existsByUsername(f.getUsername())) {
            result.rejectValue("username", "taken", "This username already exists");
        }
        if (result.hasErrors()) return form(model);

        Doctor d;
        if (creating) {
            AppUser u = users.save(new AppUser(f.getFirstName() + " " + f.getLastName(), f.getUsername().trim(),
                    encoder.encode(pwd), Role.DOCTOR));
            d = new Doctor();
            d.setUser(u);
        } else {
            d = doctors.findById(f.getId()).orElseThrow();
            AppUser u = d.getUser();
            if (u != null) {
                u.setName(f.getFirstName() + " " + f.getLastName());
                if (!pwd.isBlank()) u.setPassword(encoder.encode(pwd));
                users.save(u);
            }
        }
        d.setFirstName(f.getFirstName().trim());
        d.setLastName(f.getLastName().trim());
        d.setGender(f.getGender());
        d.setContactNumber(f.getContactNumber());
        d.setEmail(f.getEmail());
        d.setQualification(f.getQualification());
        d.setDoctorType(f.getDoctorType());
        d.setConsultationFee(f.getConsultationFee());
        doctors.save(d);
        ra.addFlashAttribute("success", "Doctor profile saved.");
        return "redirect:/secretary/doctors/" + d.getId() + "/edit";
    }

}
