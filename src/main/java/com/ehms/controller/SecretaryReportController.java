package com.ehms.controller;

import com.ehms.dto.ReportForm;
import com.ehms.model.Report;
import com.ehms.repository.ReportRepository;
import com.ehms.web.Qs;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/secretary/reports")
public class SecretaryReportController {

    private final ReportRepository reports;

    public SecretaryReportController(ReportRepository reports) { this.reports = reports; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", reports.search(null, "", Pageable.unpaged()).getContent());
        return "secretary/reports";
    }

    private String formView(Model model, Report report, ReportForm form) {
        model.addAttribute("report", report);
        model.addAttribute("form", form);
        return "secretary/report-form";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Report r = reports.findById(id).orElse(null);
        if (r == null) { ra.addFlashAttribute("error", "Report not found."); return "redirect:/secretary/reports"; }
        ReportForm f = new ReportForm();
        f.setDiagnosis(r.getDiagnosis());
        f.setMedicinePrescribed(r.getMedicinePrescribed());
        f.setDoctorComment(r.getDoctorComment());
        return formView(model, r, f);
    }

    @PostMapping("/{id}")
    public String save(@PathVariable Long id, @Valid @ModelAttribute("form") ReportForm f, BindingResult result,
                       Model model, RedirectAttributes ra) {
        Report r = reports.findById(id).orElseThrow();
        if (result.hasErrors()) return formView(model, r, f);
        r.setDiagnosis(f.getDiagnosis());
        r.setMedicinePrescribed(f.getMedicinePrescribed());
        r.setDoctorComment(f.getDoctorComment());
        reports.save(r);
        ra.addFlashAttribute("success", "Report updated.");
        return "redirect:/secretary/reports";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        reports.deleteById(id);
        ra.addFlashAttribute("success", "Report deleted.");
        return "redirect:/secretary/reports";
    }
}
