package com.ehms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.time.Duration;
import java.util.Locale;

/** The language is chosen with ?lang=fr|ar|en and remembered in a cookie. */
@Configuration
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver(@Value("${clinic.default-language:fr}") String defaultLanguage) {
        CookieLocaleResolver r = new CookieLocaleResolver("clinic-lang");
        r.setDefaultLocale(Locale.forLanguageTag(defaultLanguage));
        r.setCookieMaxAge(Duration.ofDays(365));
        return r;
    }
}
