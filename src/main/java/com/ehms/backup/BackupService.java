package com.ehms.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Daily backup of the whole database into a ZIP that contains one .sql file
 * (CREATE TABLE + INSERT statements). Pure JDBC: it does not need mysqldump.
 *
 * A check runs every hour and one minute after start-up: if no backup exists for today yet,
 * one is created. This works even when the computer is switched off at night.
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");
    /** Only files with this exact shape can be downloaded (blocks path tricks like ../). */
    public static final Pattern FILE_NAME = Pattern.compile("clinic_backup_\\d{4}-\\d{2}-\\d{2}_\\d{6}\\.sql\\.zip");

    private final DataSource dataSource;
    private final boolean enabled;
    private final Path dir;
    private final String extraDir;
    private final int keep;

    private volatile String lastError;
    private volatile LocalDateTime lastRun;

    public BackupService(DataSource dataSource,
                         @Value("${backup.enabled:true}") boolean enabled,
                         @Value("${backup.dir:./backups}") String dir,
                         @Value("${backup.extra-dir:}") String extraDir,
                         @Value("${backup.keep:30}") int keep) {
        this.dataSource = dataSource;
        this.enabled = enabled;
        this.dir = Paths.get(dir).toAbsolutePath().normalize();
        this.extraDir = extraDir == null ? "" : extraDir.trim();
        this.keep = Math.max(keep, 1);
    }

    public boolean isEnabled() { return enabled; }
    public Path getDir() { return dir; }
    public String getExtraDir() { return extraDir; }
    public int getKeep() { return keep; }
    public String getLastError() { return lastError; }
    public LocalDateTime getLastRun() { return lastRun; }

    // ------------------------------------------------------------------ scheduling

    @Scheduled(initialDelay = 60_000, fixedDelay = 3_600_000)
    public void ensureDailyBackup() {
        if (!enabled) return;
        try {
            if (!hasBackupForDay(LocalDate.now())) createBackup();
        } catch (Exception e) {
            log.error("Daily backup check failed", e);
        }
    }

    private boolean hasBackupForDay(LocalDate day) {
        String prefix = "clinic_backup_" + day + "_";
        return list().stream().anyMatch(b -> b.getName().startsWith(prefix));
    }

    // ------------------------------------------------------------------ public API

    public static final class BackupFile {
        private final String name;
        private final long size;
        private final LocalDateTime modified;

        public BackupFile(String name, long size, LocalDateTime modified) {
            this.name = name;
            this.size = size;
            this.modified = modified;
        }

        public String getName() { return name; }
        public long getSize() { return size; }
        public LocalDateTime getModified() { return modified; }
    }

    /** Existing backups, newest first. */
    public List<BackupFile> list() {
        List<BackupFile> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> FILE_NAME.matcher(p.getFileName().toString()).matches()).forEach(p -> {
                try {
                    out.add(new BackupFile(p.getFileName().toString(), Files.size(p),
                            LocalDateTime.ofInstant(Files.getLastModifiedTime(p).toInstant(),
                                    java.time.ZoneId.systemDefault())));
                } catch (IOException ignored) { }
            });
        } catch (IOException e) {
            log.warn("Cannot list backups: {}", e.getMessage());
        }
        out.sort((a, b) -> b.getName().compareTo(a.getName()));
        return out;
    }

    /** Resolves a backup name to a file, or null if the name is not a real backup. */
    public Path resolve(String name) {
        if (name == null || !FILE_NAME.matcher(name).matches()) return null;
        Path p = dir.resolve(name).normalize();
        return p.startsWith(dir) && Files.isRegularFile(p) ? p : null;
    }

    /** Creates a backup now. Returns the file name. */
    public synchronized String createBackup() throws Exception {
        Files.createDirectories(dir);
        String name = "clinic_backup_" + LocalDateTime.now().format(STAMP) + ".sql.zip";
        Path tmp = dir.resolve(name + ".part");
        Path target = dir.resolve(name);
        try {
            try (OutputStream fo = Files.newOutputStream(tmp);
                 ZipOutputStream zip = new ZipOutputStream(fo, StandardCharsets.UTF_8)) {
                zip.putNextEntry(new ZipEntry(name.replace(".zip", "")));
                Writer w = new BufferedWriter(new OutputStreamWriter(zip, StandardCharsets.UTF_8));
                dump(w);
                w.flush();
                zip.closeEntry();
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            lastError = null;
        } catch (Exception e) {
            Files.deleteIfExists(tmp);
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            lastRun = LocalDateTime.now();
            log.error("Backup failed", e);
            throw e;
        }
        lastRun = LocalDateTime.now();
        log.info("Backup created: {}", target);
        copyToExtra(target);
        prune();
        return name;
    }

    // ------------------------------------------------------------------ internals

    private void copyToExtra(Path file) {
        if (extraDir.isEmpty()) return;
        try {
            Path d = Paths.get(extraDir).toAbsolutePath().normalize();
            Files.createDirectories(d);
            Files.copy(file, d.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            List<Path> old;
            try (Stream<Path> s = Files.list(d)) {
                old = s.filter(p -> FILE_NAME.matcher(p.getFileName().toString()).matches())
                        .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                        .collect(java.util.stream.Collectors.toList());
            }
            for (int i = keep; i < old.size(); i++) Files.deleteIfExists(old.get(i));
        } catch (Exception e) {
            // the main backup is fine; only the second copy failed
            lastError = "Second copy failed (" + extraDir + "): " + e.getMessage();
            log.warn(lastError);
        }
    }

    private void prune() {
        List<BackupFile> all = list();
        for (int i = keep; i < all.size(); i++) {
            try { Files.deleteIfExists(dir.resolve(all.get(i).getName())); } catch (IOException ignored) { }
        }
    }

    private void dump(Writer w) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            String catalog = c.getCatalog();
            w.write("-- Clinic backup " + LocalDateTime.now() + "\n");
            w.write("-- Database: " + catalog + "\n");
            w.write("-- Restore: mysql -u USER -p " + catalog + " < this_file.sql\n\n");
            w.write("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n\n");

            List<String> tables = new ArrayList<>();
            try (ResultSet rs = c.getMetaData().getTables(catalog, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
            }
            for (String t : tables) dumpTable(c, w, t);
            w.write("SET FOREIGN_KEY_CHECKS=1;\n");
        }
    }

    private void dumpTable(Connection c, Writer w, String table) throws Exception {
        String q = "`" + table.replace("`", "``") + "`";
        try (Statement st = c.createStatement()) {
            try (ResultSet rs = st.executeQuery("SHOW CREATE TABLE " + q)) {
                if (rs.next()) {
                    w.write("DROP TABLE IF EXISTS " + q + ";\n");
                    w.write(rs.getString(2) + ";\n\n");
                }
            }
        }
        try (Statement st = c.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            try (ResultSet rs = st.executeQuery("SELECT * FROM " + q)) {
                int cols = rs.getMetaData().getColumnCount();
                StringBuilder head = new StringBuilder("INSERT INTO " + q + " (");
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) head.append(", ");
                    head.append('`').append(rs.getMetaData().getColumnLabel(i).replace("`", "``")).append('`');
                }
                head.append(") VALUES ");
                int n = 0;
                while (rs.next()) {
                    StringBuilder row = new StringBuilder(head).append('(');
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) row.append(", ");
                        row.append(literal(rs, i));
                    }
                    row.append(");\n");
                    w.write(row.toString());
                    n++;
                }
                if (n > 0) w.write("\n");
            }
        }
    }

    private static String literal(ResultSet rs, int i) throws SQLException {
        Object o = rs.getObject(i);
        if (o == null) return "NULL";
        if (o instanceof byte[] b) {
            if (b.length == 0) return "''";
            StringBuilder sb = new StringBuilder("0x");
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        }
        if (o instanceof Boolean bo) return bo ? "1" : "0";
        if (o instanceof java.math.BigDecimal bd) return bd.toPlainString();
        if (o instanceof Number) return o.toString();
        return quote(rs.getString(i));
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2).append('\'');
        for (int k = 0; k < s.length(); k++) {
            char ch = s.charAt(k);
            switch (ch) {
                case '\\' -> sb.append("\\\\");
                case '\'' -> sb.append("\\'");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\0' -> sb.append("\\0");
                case '\u001a' -> sb.append("\\Z");
                default -> sb.append(ch);
            }
        }
        return sb.append('\'').toString();
    }
}
