package com.ehms.controller;

import com.ehms.license.LicenseService;
import com.ehms.license.LicenseStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/secretary/license")
public class SecretaryLicenseController {

    private final LicenseService licenses;

    public SecretaryLicenseController(LicenseService licenses) { this.licenses = licenses; }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("license", licenses.status());
        model.addAttribute("machineId", licenses.machineId());
        model.addAttribute("history", licenses.history());
        return "secretary/license";
    }

    @PostMapping
    public String save(@RequestParam String key, RedirectAttributes ra) {
        LicenseStatus s = licenses.activate(key);
        if (s.isValid()) ra.addFlashAttribute("success", "License saved.");
        else ra.addFlashAttribute("error", s.getMessage());
        return "redirect:/secretary/license";
    }
}
