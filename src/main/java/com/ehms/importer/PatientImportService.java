package com.ehms.importer;

import com.ehms.model.Patient;
import com.ehms.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/** Imports patients from a CSV file (comma, semicolon or tab separated, UTF-8). */
@Service
public class PatientImportService {

    public static final String TEMPLATE =
            "first_name;last_name;gender;date_of_birth;phone;note\r\n"
            + "Amine;Ben Ali;M;1985-03-14;20123456;Example row - delete it before importing\r\n"
            + "Sonia;Trabelsi;F;14/07/1990;98123456;\r\n";

    private static final int MAX_ROWS = 20000;
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy"));

    private final PatientRepository patients;

    public PatientImportService(PatientRepository patients) { this.patients = patients; }

    @Transactional
    public ImportResult importCsv(byte[] content) {
        ImportResult result = new ImportResult();
        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);

        List<List<String>> rows = parse(text, detectDelimiter(text));
        if (rows.isEmpty()) {
            result.error(1, "The file is empty.");
            return result;
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> header = rows.get(0);
        for (int i = 0; i < header.size(); i++) col.putIfAbsent(canonical(header.get(i)), i);
        for (String required : List.of("first", "last", "gender", "dob", "phone")) {
            if (!col.containsKey(required)) {
                result.error(1, "Column missing or misspelled: " + (switch (required) {
                    case "first" -> "first_name";
                    case "last" -> "last_name";
                    case "dob" -> "date_of_birth";
                    default -> required;
                }) + ". Use the template.");
                return result;
            }
        }

        Set<String> seen = new HashSet<>();
        List<Patient> toSave = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            int line = r + 1;
            if (row.stream().allMatch(String::isBlank)) continue;
            if (result.getTotal() >= MAX_ROWS) { result.error(line, "Too many rows (maximum " + MAX_ROWS + ")."); break; }
            result.row();

            String first = cell(row, col.get("first"));
            String last = cell(row, col.get("last"));
            String phone = cell(row, col.get("phone"));
            String note = col.containsKey("note") ? cell(row, col.get("note")) : "";
            String gender = parseGender(cell(row, col.get("gender")));
            LocalDate dob = parseDate(cell(row, col.get("dob")));

            if (first.isEmpty()) { result.error(line, "first name is empty."); continue; }
            if (last.isEmpty()) { result.error(line, "last name is empty."); continue; }
            if (gender == null) { result.error(line, "gender must be M or F (or Male / Female)."); continue; }
            if (dob == null) { result.error(line, "date of birth is missing or not understood (use yyyy-MM-dd or dd/MM/yyyy)."); continue; }
            if (dob.isAfter(LocalDate.now())) { result.error(line, "date of birth is in the future."); continue; }
            if (phone.isEmpty()) { result.error(line, "phone is empty."); continue; }
            if (first.length() > 100 || last.length() > 100 || phone.length() > 50 || note.length() > 2000) {
                result.error(line, "a value is too long."); continue;
            }

            String key = (first + "|" + last + "|" + dob).toLowerCase(Locale.ROOT);
            if (!seen.add(key) || patients.existsByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirth(first, last, dob)) {
                result.duplicate();
                continue;
            }
            Patient p = new Patient();
            p.setFirstName(first);
            p.setLastName(last);
            p.setGender(gender);
            p.setDateOfBirth(dob);
            p.setContactNumber(phone);
            p.setNotes(note.isEmpty() ? null : note);
            toSave.add(p);
            result.imported();
        }
        patients.saveAll(toSave);
        return result;
    }

    private static String cell(List<String> row, Integer idx) {
        return idx == null || idx >= row.size() ? "" : row.get(idx).trim();
    }

    private static String canonical(String header) {
        String h = header == null ? "" : header.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return switch (h) {
            case "firstname", "first", "prenom", "givenname" -> "first";
            case "lastname", "last", "nom", "surname", "familyname" -> "last";
            case "gender", "sex", "sexe" -> "gender";
            case "dateofbirth", "dob", "birthdate", "birthday", "datedenaissance", "naissance" -> "dob";
            case "phone", "contactnumber", "tel", "telephone", "mobile" -> "phone";
            case "note", "notes", "remark", "remarks" -> "note";
            default -> h;
        };
    }

    private static String parseGender(String g) {
        return switch (g.toLowerCase(Locale.ROOT)) {
            case "m", "male", "man", "h", "homme" -> "M";
            case "f", "female", "woman", "femme" -> "F";
            default -> null;
        };
    }

    private static LocalDate parseDate(String s) {
        if (s.isEmpty()) return null;
        for (DateTimeFormatter f : DATE_FORMATS) {
            try { return LocalDate.parse(s, f); } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    private static char detectDelimiter(String text) {
        int end = text.indexOf('\n');
        String first = end < 0 ? text : text.substring(0, end);
        int semi = count(first, ';'), comma = count(first, ','), tab = count(first, '\t');
        if (tab > semi && tab > comma) return '\t';
        return semi >= comma ? ';' : ',';
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }

    /** Minimal RFC 4180 parser: quotes, doubled quotes, delimiters and line breaks inside quotes. */
    static List<List<String>> parse(String text, char delimiter) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else quoted = false;
                } else cur.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == delimiter) {
                row.add(cur.toString()); cur.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cur.toString()); cur.setLength(0);
                rows.add(row); row = new ArrayList<>();
            } else cur.append(c);
        }
        if (cur.length() > 0 || !row.isEmpty()) { row.add(cur.toString()); rows.add(row); }
        return rows;
    }
}
