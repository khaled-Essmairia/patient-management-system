package com.ehms.controller;

import com.ehms.importer.PatientImportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/secretary/import")
public class SecretaryImportController {

    private final PatientImportService service;

    public SecretaryImportController(PatientImportService service) { this.service = service; }

    @GetMapping
    public String page() { return "secretary/import-patients"; }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}); // UTF-8 BOM so Excel reads accents
        out.writeBytes(PatientImportService.TEMPLATE.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("patients-template.csv").build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(out.toByteArray());
    }

    @PostMapping
    public String upload(@RequestParam("file") MultipartFile file, RedirectAttributes ra) throws Exception {
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Choose a CSV file first.");
            return "redirect:/secretary/import";
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".csv") && !name.endsWith(".txt")) {
            ra.addFlashAttribute("error", "The file must be a .csv file.");
            return "redirect:/secretary/import";
        }
        ra.addFlashAttribute("importResult", service.importCsv(file.getBytes()));
        return "redirect:/secretary/import";
    }
}
