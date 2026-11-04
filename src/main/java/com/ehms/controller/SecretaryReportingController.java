package com.ehms.controller;

import com.ehms.reporting.ReportingService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

@Controller
@RequestMapping("/secretary/reporting")
public class SecretaryReportingController {

    private final ReportingService reporting;

    public SecretaryReportingController(ReportingService reporting) { this.reporting = reporting; }

    @GetMapping
    public String page(Model model) throws SQLException {
        model.addAttribute("tables", reporting.tables());
        return "secretary/reporting";
    }

    @GetMapping("/export/{table}")
    public void exportTable(@PathVariable String table, HttpServletResponse response) throws SQLException, IOException {
        if (!reporting.exists(table)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + table + "-" + LocalDate.now() + ".csv\"");
        reporting.writeCsv(table, response.getOutputStream());
    }

    @GetMapping("/export-all")
    public void exportAll(HttpServletResponse response) throws SQLException, IOException {
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"clinic-database-" + LocalDate.now() + ".zip\"");
        reporting.writeZip(response.getOutputStream());
    }
}
