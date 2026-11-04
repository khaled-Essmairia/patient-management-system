package com.ehms.license;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/** Without a valid license, every page redirects to License Management (secretary) or to an information page (others). */
public class LicenseInterceptor implements HandlerInterceptor {

    private final LicenseService licenses;

    public LicenseInterceptor(LicenseService licenses) { this.licenses = licenses; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (licenses.status().isValid()) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean secretary = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SECRETARY".equals(a.getAuthority()));
        response.sendRedirect(request.getContextPath() + (secretary ? "/secretary/license" : "/license-required"));
        return false;
    }
}
