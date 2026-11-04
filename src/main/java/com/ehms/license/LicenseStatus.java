package com.ehms.license;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

/** Result of checking a license key (or the stored license). */
@Getter @AllArgsConstructor
public class LicenseStatus {
    private final boolean valid;
    private final String message;
    private final String customer;
    private final LocalDate expires;
    private final boolean bound;
    private final long daysLeft;

    public static LicenseStatus invalid(String message) {
        return new LicenseStatus(false, message, null, null, false, 0);
    }
}
