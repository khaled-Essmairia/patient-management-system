package com.ehms.web;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Null-safe formatting helpers, used in templates as ${@fmt.money(x)}. Dates follow the language of the user. */
@Component("fmt")
public class Fmt {
    private static final Locale AR = Locale.forLanguageTag("ar-TN");

    private static Locale loc() {
        Locale l = LocaleContextHolder.getLocale();
        if ("ar".equals(l.getLanguage())) return AR;
        if ("fr".equals(l.getLanguage())) return Locale.FRANCE;
        return Locale.ENGLISH;
    }

    public String money(BigDecimal v) {
        if (v == null) return "-";
        Locale l = loc();
        return String.format("ar".equals(l.getLanguage()) ? Locale.US : l, "%,.2f", v);
    }

    public String date(LocalDate d) {
        return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("dd MMM yyyy", loc()));
    }

    public String dateTime(LocalDateTime d) {
        return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", loc()));
    }

    public LocalDate today() { return LocalDate.now(); }

    public String time(LocalDateTime d) {
        return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
