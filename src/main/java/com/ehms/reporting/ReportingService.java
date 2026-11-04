package com.ehms.reporting;

import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Exports every table of the database to CSV (one table, or all tables in a ZIP). Password hashes are never exported. */
@Service
public class ReportingService {

    public record TableInfo(String name, long rows) {
        public String getName() { return name; }
        public long getRows() { return rows; }
    }

    private final DataSource dataSource;

    public ReportingService(DataSource dataSource) { this.dataSource = dataSource; }

    public List<TableInfo> tables() throws SQLException {
        List<TableInfo> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection()) {
            String q = c.getMetaData().getIdentifierQuoteString();
            for (String name : tableNames(c)) {
                long count = 0;
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("select count(*) from " + q + name + q)) {
                    if (rs.next()) count = rs.getLong(1);
                }
                list.add(new TableInfo(name, count));
            }
        }
        return list;
    }

    public boolean exists(String table) throws SQLException {
        try (Connection c = dataSource.getConnection()) {
            return tableNames(c).contains(table);
        }
    }

    public void writeCsv(String table, OutputStream out) throws SQLException, IOException {
        try (Connection c = dataSource.getConnection()) {
            if (!tableNames(c).contains(table)) throw new IllegalArgumentException("Unknown table");
            Writer w = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            writeTable(c, table, w);
            w.flush();
        }
    }

    public void writeZip(OutputStream out) throws SQLException, IOException {
        try (Connection c = dataSource.getConnection()) {
            ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
            Writer w = new OutputStreamWriter(zip, StandardCharsets.UTF_8);
            for (String table : tableNames(c)) {
                zip.putNextEntry(new ZipEntry(table + ".csv"));
                writeTable(c, table, w);
                w.flush();
                zip.closeEntry();
            }
            zip.finish();
            zip.flush();
        }
    }

    private List<String> tableNames(Connection c) throws SQLException {
        List<String> names = new ArrayList<>();
        try (ResultSet rs = c.getMetaData().getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) names.add(rs.getString("TABLE_NAME"));
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    private void writeTable(Connection c, String table, Writer w) throws SQLException, IOException {
        String q = c.getMetaData().getIdentifierQuoteString();
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("select * from " + q + table + q)) {
            ResultSetMetaData md = rs.getMetaData();
            List<Integer> cols = new ArrayList<>();
            w.write('\uFEFF'); // UTF-8 BOM so Excel reads accents
            for (int i = 1; i <= md.getColumnCount(); i++) {
                if (md.getColumnLabel(i).toLowerCase(Locale.ROOT).contains("password")) continue;
                if (!cols.isEmpty()) w.write(';');
                w.write(escape(md.getColumnLabel(i)));
                cols.add(i);
            }
            w.write("\r\n");
            while (rs.next()) {
                boolean first = true;
                for (int i : cols) {
                    if (!first) w.write(';');
                    first = false;
                    String v = rs.getString(i);
                    w.write(v == null ? "" : escape(v));
                }
                w.write("\r\n");
            }
        }
    }

    private static String escape(String v) {
        if (v.contains(";") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }
}
