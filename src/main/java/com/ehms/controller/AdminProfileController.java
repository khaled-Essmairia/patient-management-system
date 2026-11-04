package com.ehms.controller;

import com.ehms.dto.PasswordForm;
import com.ehms.model.AppUser;
import com.ehms.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** System administration > Admin profile: change the password of the signed-in administrator (the secretary). */
@Controller
@RequestMapping("/secretary/admin/profile")
public class AdminProfileController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminProfileController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    private String view(Model model, Authentication auth, PasswordForm form) {
        AppUser u = users.findByUsername(auth.getName()).orElseThrow();
        model.addAttribute("account", u);
        model.addAttribute("form", form);
        return "secretary/admin-profile";
    }

    @GetMapping
    public String page(Authentication auth, Model model) {
        return view(model, auth, new PasswordForm());
    }

    @PostMapping
    public String change(@Valid @ModelAttribute("form") PasswordForm form, BindingResult result,
                         Authentication auth, Model model, RedirectAttributes ra) {
        AppUser u = users.findByUsername(auth.getName()).orElseThrow();
        if (!result.hasFieldErrors("oldPassword") && !encoder.matches(form.getOldPassword(), u.getPassword())) {
            result.rejectValue("oldPassword", "wrong", "The old password is not correct");
        }
        if (!result.hasFieldErrors("newPassword")) {
            if (form.getNewPassword().length() < 6) {
                result.rejectValue("newPassword", "short", "The new password must have at least 6 characters");
            } else if (form.getNewPassword().equals(form.getOldPassword())) {
                result.rejectValue("newPassword", "same", "The new password must be different from the old one");
            }
        }
        if (!result.hasFieldErrors("confirmPassword") && !form.getNewPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "The confirmation does not match the new password");
        }
        if (result.hasErrors()) return view(model, auth, form);
        u.setPassword(encoder.encode(form.getNewPassword()));
        users.save(u);
        ra.addFlashAttribute("success", "Password changed.");
        return "redirect:/secretary/admin/profile";
    }
}
