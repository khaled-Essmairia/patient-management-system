package com.ehms.controller;

import com.ehms.dto.InvoiceForm;
import com.ehms.model.*;
import com.ehms.repository.*;
import com.ehms.service.LineMapper;
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

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/secretary/invoices")
public class SecretaryInvoiceController {

    private final InvoiceRepository invoices;
    private final PatientRepository patients;
    private final LineMapper mapper;

    public SecretaryInvoiceController(InvoiceRepository invoices, PatientRepository patients, LineMapper mapper) {
        this.invoices = invoices;
        this.patients = patients;
        this.mapper = mapper;
    }

    private static BigDecimal sum(List<Invoice> list) {
        return list.stream().map(Invoice::getTotalTtc).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping
    public String list(@RequestParam(required = false) InvoiceStatus status, Model model) {
        model.addAttribute("rows", invoices.search(status, "", Pageable.unpaged()).getContent());
        model.addAttribute("status", status);
        model.addAttribute("statuses", InvoiceStatus.values());
        model.addAttribute("unpaidTotal", sum(invoices.findByStatus(InvoiceStatus.UNPAID)));
        model.addAttribute("paidTotal", sum(invoices.findByStatus(InvoiceStatus.PAID)));
        return "secretary/invoices";
    }

    private String formView(Model model, InvoiceForm form) {
        model.addAttribute("form", form);
        model.addAttribute("patients", patients.findAllByOrderByLastNameAscFirstNameAsc());
        model.addAttribute("paymentMethods", PaymentMethod.values());
        model.addAttribute("statuses", InvoiceStatus.values());
        Invoice i = form.getId() == null ? null : invoices.findById(form.getId()).orElse(null);
        model.addAttribute("ref", i == null ? "Generated on save" : i.getReference());
        return "secretary/invoice-form";
    }

    @GetMapping("/new")
    public String create(@RequestParam(required = false) Long patientId, Model model) {
        InvoiceForm f = new InvoiceForm();
        f.setPatientId(patientId);
        return formView(model, f);
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Invoice i = invoices.findById(id).orElse(null);
        if (i == null) { ra.addFlashAttribute("error", "Invoice not found."); return "redirect:/secretary/invoices"; }
        InvoiceForm f = mapper.fill(new InvoiceForm(), i);
        f.setPatientId(i.getPatient().getId());
        return formView(model, f);
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") InvoiceForm f, BindingResult result,
                       Model model, RedirectAttributes ra) {
        if (result.hasErrors()) return formView(model, f);
        Invoice i = f.getId() == null ? new Invoice() : invoices.findById(f.getId()).orElseThrow();
        i.setPatient(patients.findById(f.getPatientId()).orElseThrow());
        mapper.apply(i, f);
        invoices.save(i);
        ra.addFlashAttribute("success", "Invoice saved.");
        return "redirect:/secretary/invoices";
    }

    @GetMapping("/{id}/print")
    public String print(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Invoice i = invoices.findById(id).orElse(null);
        if (i == null) { ra.addFlashAttribute("error", "Invoice not found."); return "redirect:/secretary/invoices"; }
        model.addAttribute("invoice", i);
        model.addAttribute("receipt", false);
        return "print/invoice";
    }

    @GetMapping("/{id}/receipt")
    public String receipt(@PathVariable Long id, Model model, RedirectAttributes ra) {
        Invoice i = invoices.findById(id).orElse(null);
        if (i == null) { ra.addFlashAttribute("error", "Invoice not found."); return "redirect:/secretary/invoices"; }
        if (i.getStatus() != InvoiceStatus.PAID) {
            ra.addFlashAttribute("error", "A receipt can only be printed for a paid invoice.");
            return "redirect:/secretary/invoices";
        }
        model.addAttribute("invoice", i);
        model.addAttribute("receipt", true);
        return "print/invoice";
    }

    @PostMapping("/{id}/pay")
    public String markPaid(@PathVariable Long id, RedirectAttributes ra) {
        invoices.findById(id).ifPresent(i -> { i.setStatus(InvoiceStatus.PAID); invoices.save(i); });
        ra.addFlashAttribute("success", "Invoice marked as paid.");
        return "redirect:/secretary/invoices";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        invoices.deleteById(id);
        ra.addFlashAttribute("success", "Invoice deleted.");
        return "redirect:/secretary/invoices";
    }
}
