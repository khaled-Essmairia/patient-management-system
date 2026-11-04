package com.ehms.controller;

import com.ehms.backup.BackupService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;

@Controller
@RequestMapping("/secretary/backups")
public class SecretaryBackupController {

    private final BackupService backups;

    public SecretaryBackupController(BackupService backups) { this.backups = backups; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rows", backups.list());
        model.addAttribute("service", backups);
        return "secretary/backups";
    }

    @PostMapping("/run")
    public String run(RedirectAttributes ra) {
        try {
            backups.createBackup();
            ra.addFlashAttribute("success", "Backup created.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "The backup failed: " + e.getMessage());
        }
        return "redirect:/secretary/backups";
    }

    @GetMapping("/download/{name:.+}")
    public ResponseEntity<Resource> download(@PathVariable String name) {
        Path file = backups.resolve(name);
        if (file == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(new FileSystemResource(file));
    }
}
