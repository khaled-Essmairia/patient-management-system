package com.ehms.web;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Translates a short English text (enum label, flash message, validation message) into the language of the
 * current user. The key is "t." + the English text in lowercase with every run of other characters turned
 * into "_". When no translation exists the English text is shown as it is. Used as ${@tr.t(x)}.
 */
@Component("tr")
public class Tr {

    private final MessageSource messages;

    public Tr(MessageSource messages) { this.messages = messages; }

    public static String key(String english) {
        return "t." + english.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public String t(String english) {
        if (english == null || english.isEmpty()) return english;
        Locale locale = LocaleContextHolder.getLocale();
        String found = messages.getMessage(key(english), null, null, locale);
        if (found != null) return found;
        // "The backup failed: <technical detail>" -> translate the part before ": "
        int i = english.indexOf(": ");
        if (i > 0) {
            String head = messages.getMessage(key(english.substring(0, i)), null, null, locale);
            if (head != null) return head + english.substring(i);
        }
        return english;
    }
}
