package com.ehms.web;

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/** Builds the "&k=v" tail used by pagination links so filters survive page changes. */
public final class Qs {
    private Qs() { }

    public static String of(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i + 1] != null && !kv[i + 1].isBlank()) {
                sb.append('&').append(kv[i]).append('=').append(UriUtils.encode(kv[i + 1], StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }
}
