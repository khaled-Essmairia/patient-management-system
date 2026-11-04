package com.ehms.license;

import com.ehms.model.License;
import com.ehms.repository.LicenseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Checks license keys made by license-generator.html.
 * A key is  base64url(JSON payload) + "." + base64url(ECDSA P-256 / SHA-256 signature, raw r||s).
 * The application only holds the public key: it can check a license but never create one.
 */
@Service
public class LicenseService {

    private static final long CACHE_MILLIS = 30_000;

    private final LicenseRepository licenses;
    private final ObjectMapper mapper = new ObjectMapper();
    private final PublicKey publicKey;

    private volatile LicenseStatus cached;
    private volatile long cachedAt;
    private volatile String machineId;

    public LicenseService(LicenseRepository licenses, @Value("${clinic.license.public-key:}") String publicKeyB64) {
        this.licenses = licenses;
        PublicKey key = null;
        try {
            key = KeyFactory.getInstance("EC").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyB64.trim())));
        } catch (Exception ignored) {
            // no usable public key: every license will be refused
        }
        this.publicKey = key;
    }

    /** Status of the license currently stored (the newest one). Cached for a few seconds. */
    public LicenseStatus status() {
        long now = System.currentTimeMillis();
        LicenseStatus s = cached;
        if (s != null && now - cachedAt < CACHE_MILLIS) return s;
        s = licenses.findTopByOrderByIdDesc()
                .map(l -> verify(l.getLicenseKey()))
                .orElseGet(() -> LicenseStatus.invalid("No license has been entered yet."));
        cached = s;
        cachedAt = now;
        return s;
    }

    public void evict() { cached = null; }

    /** Checks a key and, if it is valid, stores it as the new active license. */
    public LicenseStatus activate(String rawKey) {
        String key = clean(rawKey);
        LicenseStatus s = verify(key);
        if (!s.isValid()) return s;
        License l = new License();
        l.setLicenseKey(key);
        l.setCustomer(s.getCustomer());
        l.setExpiresOn(s.getExpires());
        l.setActivatedAt(LocalDateTime.now());
        licenses.save(l);
        evict();
        return s;
    }

    public List<License> history() { return licenses.findAllByOrderByIdDesc(); }

    public LicenseStatus verify(String rawKey) {
        String key = clean(rawKey);
        if (publicKey == null) return LicenseStatus.invalid("The public key of the application is missing or wrong (clinic.license.public-key).");
        int dot = key.indexOf('.');
        if (dot <= 0 || dot != key.lastIndexOf('.') || dot == key.length() - 1) {
            return LicenseStatus.invalid("This is not a valid license key.");
        }
        String payloadB64 = key.substring(0, dot);
        try {
            byte[] sig = Base64.getUrlDecoder().decode(key.substring(dot + 1));
            Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
            verifier.initVerify(publicKey);
            verifier.update(payloadB64.getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(sig)) return LicenseStatus.invalid("This license key is not genuine.");

            JsonNode p = mapper.readTree(Base64.getUrlDecoder().decode(payloadB64));
            LocalDate exp = LocalDate.parse(p.path("exp").asText());
            String name = p.path("name").asText("");
            String mid = p.path("mid").asText("").trim();
            boolean bound = !mid.isEmpty();
            if (bound && !mid.equalsIgnoreCase(machineId())) {
                return new LicenseStatus(false, "This license was issued for another computer.", name, exp, true, 0);
            }
            long days = ChronoUnit.DAYS.between(LocalDate.now(), exp);
            if (days < 0) {
                return new LicenseStatus(false, "This license expired on " + exp + ".", name, exp, bound, days);
            }
            return new LicenseStatus(true, "OK", name, exp, bound, days);
        } catch (Exception e) {
            return LicenseStatus.invalid("This is not a valid license key.");
        }
    }

    /** Stable id of this computer, shown as XXXX-XXXX-XXXX-XXXX. Built from the physical network cards. */
    public String machineId() {
        String id = machineId;
        if (id == null) {
            id = computeMachineId();
            machineId = id;
        }
        return id;
    }

    private static String clean(String s) { return s == null ? "" : s.replaceAll("\\s+", ""); }

    private static String computeMachineId() {
        List<String> macs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets != null && nets.hasMoreElements()) {
                NetworkInterface ni = nets.nextElement();
                byte[] mac = ni.getHardwareAddress();
                String name = (ni.getDisplayName() + " " + ni.getName()).toLowerCase(Locale.ROOT);
                if (mac == null || mac.length == 0 || ni.isLoopback() || ni.isVirtual()) continue;
                if (name.matches(".*(virtual|vmware|vbox|docker|hyper-v|vethernet|vpn|tap|tun|veth|bridge|br-|loopback|bluetooth).*")) continue;
                StringBuilder sb = new StringBuilder();
                for (byte b : mac) sb.append(String.format("%02X", b));
                macs.add(sb.toString());
            }
        } catch (Exception ignored) {
            // fall through to the fallback below
        }
        Collections.sort(macs);
        String seed = macs.isEmpty()
                ? "nomac|" + System.getProperty("os.name") + "|" + System.getenv().getOrDefault("COMPUTERNAME", System.getenv().getOrDefault("HOSTNAME", "host"))
                : String.join(",", macs) + "|" + System.getProperty("os.name");
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(seed.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < 8; i++) hex.append(String.format("%02X", h[i]));
            return hex.substring(0, 4) + "-" + hex.substring(4, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16);
        } catch (Exception e) {
            return "0000-0000-0000-0000";
        }
    }
}
