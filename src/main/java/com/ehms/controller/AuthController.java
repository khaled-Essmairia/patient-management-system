package com.ehms.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/")
    public String home(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";
        for (var a : auth.getAuthorities()) {
            if ("ROLE_SECRETARY".equals(a.getAuthority())) return "redirect:/secretary";
            if ("ROLE_DOCTOR".equals(a.getAuthority())) return "redirect:/doctor";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/license-required")
    public String licenseRequired() {
        return "license-required";
    }
}
