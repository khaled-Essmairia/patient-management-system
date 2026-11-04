package com.ehms.sms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Sends one SMS through the HTTP API of the SMS provider chosen in application.properties.
 * Any provider that accepts an HTTP request works (Twilio, Infobip, a local Tunisian bulk-SMS
 * gateway...): the URL, headers and body are templates with the placeholders {to}, {message}, {from}.
 * A response with an HTTP status 2xx counts as "sent".
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final boolean enabled;
    private final String url;
    private final String method;
    private final String body;
    private final String contentType;
    private final String headers;
    private final String from;
    private final String countryCode;
    private final boolean stripPlus;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public SmsService(@Value("${sms.enabled:false}") boolean enabled,
                      @Value("${sms.http.url:}") String url,
                      @Value("${sms.http.method:POST}") String method,
                      @Value("${sms.http.body:}") String body,
                      @Value("${sms.http.content-type:application/x-www-form-urlencoded}") String contentType,
                      @Value("${sms.http.headers:}") String headers,
                      @Value("${sms.from:}") String from,
                      @Value("${sms.default-country-code:216}") String countryCode,
                      @Value("${sms.strip-plus:false}") boolean stripPlus) {
        this.enabled = enabled;
        this.url = url.trim();
        this.method = method.trim().toUpperCase();
        this.body = body;
        this.contentType = contentType.trim();
        this.headers = headers;
        this.from = from;
        this.countryCode = countryCode.trim();
        this.stripPlus = stripPlus;
    }

    /** True when SMS is switched on and a provider URL is configured. */
    public boolean isReady() { return enabled && !url.isEmpty(); }
    public boolean isEnabled() { return enabled; }

    /** "98 123 456" -> "+21698123456". Returns null when nothing usable is left. */
    public String normalize(String phone) {
        if (phone == null) return null;
        String p = phone.replaceAll("[\\s.\\-()/]", "");
        if (p.isEmpty()) return null;
        if (p.startsWith("00")) p = "+" + p.substring(2);
        if (!p.startsWith("+")) {
            if (p.startsWith("0")) p = p.replaceFirst("^0+", "");
            // Tunisian numbers have 8 digits; with the country code already typed they have 11
            if (p.startsWith(countryCode) && p.length() > 8) p = "+" + p;
            else p = "+" + countryCode + p;
        }
        if (!p.substring(1).matches("\\d{8,15}")) return null;
        return stripPlus ? p.substring(1) : p;
    }

    /** Sends the message. Throws an exception with a readable message when it fails. */
    public void send(String phone, String message) throws Exception {
        if (!isReady()) throw new IllegalStateException("SMS is not configured");
        String to = normalize(phone);
        if (to == null) throw new IllegalArgumentException("Invalid phone number: " + phone);

        boolean json = contentType.toLowerCase().contains("json");
        HttpRequest.Builder rb = HttpRequest.newBuilder()
                .uri(URI.create(fill(url, to, message, Mode.URL)))
                .timeout(Duration.ofSeconds(20));
        if (!headers.isBlank()) {
            // "Name: value; Name2: value2"
            for (String h : headers.split(";")) {
                int i = h.indexOf(':');
                if (i > 0) rb.header(h.substring(0, i).trim(), fill(h.substring(i + 1).trim(), to, message, Mode.RAW));
            }
        }
        if (method.equals("GET")) {
            rb.GET();
        } else {
            rb.header("Content-Type", contentType);
            rb.method(method, HttpRequest.BodyPublishers.ofString(
                    fill(body, to, message, json ? Mode.JSON : Mode.URL), StandardCharsets.UTF_8));
        }
        HttpResponse<String> res = client.send(rb.build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() / 100 != 2) {
            String snippet = res.body() == null ? "" : res.body().substring(0, Math.min(200, res.body().length()));
            throw new IllegalStateException("SMS provider answered HTTP " + res.statusCode() + " " + snippet);
        }
        log.info("SMS sent to {}", to);
    }

    private enum Mode { URL, JSON, RAW }

    private String fill(String template, String to, String message, Mode mode) {
        return template
                .replace("{to}", enc(to, mode))
                .replace("{message}", enc(message, mode))
                .replace("{from}", enc(from, mode));
    }

    private static String enc(String v, Mode mode) {
        return switch (mode) {
            case URL -> URLEncoder.encode(v, StandardCharsets.UTF_8);
            case JSON -> v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
            case RAW -> v;
        };
    }
}
