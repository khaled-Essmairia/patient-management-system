package com.ehms.config;

import com.ehms.license.LicenseInterceptor;
import com.ehms.license.LicenseService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LicenseService licenses;

    public WebConfig(LicenseService licenses) { this.licenses = licenses; }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor lang = new LocaleChangeInterceptor();
        lang.setParamName("lang");
        registry.addInterceptor(lang);
        registry.addInterceptor(new LicenseInterceptor(licenses))
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/logout", "/css/**", "/js/**", "/error",
                        "/secretary/license", "/secretary/license/**", "/license-required");
    }
}
