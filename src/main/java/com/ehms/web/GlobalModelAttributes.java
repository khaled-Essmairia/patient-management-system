package com.ehms.web;

import com.ehms.license.LicenseService;
import com.ehms.license.LicenseStatus;
import com.ehms.model.AppUser;
import com.ehms.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserRepository users;
    private final LicenseService licenses;
    private final String clinicName;
    private final String currency;

    public GlobalModelAttributes(UserRepository users, LicenseService licenses,
                                 @Value("${clinic.name:Clinic}") String clinicName,
                                 @Value("${clinic.currency:TND}") String currency) {
        this.users = users;
        this.licenses = licenses;
        this.clinicName = clinicName;
        this.currency = currency;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) { return request.getRequestURI(); }

    @ModelAttribute("clinicName")
    public String clinicName() { return clinicName; }

    @ModelAttribute("currency")
    public String currency() { return currency; }

    @ModelAttribute("meName")
    public String meName(Authentication auth) {
        if (auth == null) return null;
        return users.findByUsername(auth.getName()).map(AppUser::getName).orElse(auth.getName());
    }

    @ModelAttribute("licenseValid")
    public boolean licenseValid() { return licenses.status().isValid(); }

    /** Days left when the license is about to end (14 days or less), otherwise null. */
    @ModelAttribute("licenseDaysLeft")
    public Long licenseDaysLeft() {
        LicenseStatus s = licenses.status();
        return s.isValid() && s.getDaysLeft() <= 14 ? s.getDaysLeft() : null;
    }
}
