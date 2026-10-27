package org.openelisglobal.addon.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.dao.AddonDAO;
import org.openelisglobal.addon.dao.AddonSecretDAO;
import org.openelisglobal.addon.packaging.AddonManifest;
import org.openelisglobal.addon.packaging.AddonSignature;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.valueholder.Addon;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddonTokenServiceImpl implements AddonTokenService {

    static final String TOKEN_KEY = "token-signing-key";

    @Autowired
    private AddonDAO addonDAO;
    @Autowired
    private AddonSecretDAO secretDAO;
    @Autowired
    private AddonSettings settings;
    @Autowired
    private SampleService sampleService;

    private volatile byte[] tokenKey;

    @Override
    public IssuedToken issue(String addonId, String slot, Map<String, ?> context, String userId,
            Collection<String> userRoles) {
        Addon addon = addonDAO.get(addonId).filter(Addon::isEnabled)
                .orElseThrow(() -> new TokenRefusedException(Refusal.NOT_FOUND));
        boolean contributes = AddonManifest.parse(addon.getManifest()).extensions().stream()
                .anyMatch(e -> e.slot().equals(slot));
        AddonCatalog.Slot known = AddonCatalog.slot(slot);
        if (!contributes || known == null) {
            throw new TokenRefusedException(Refusal.SLOT);
        }
        if (!known.roles().isEmpty() && known.roles().stream().noneMatch(userRoles::contains)) {
            throw new TokenRefusedException(Refusal.ROLE);
        }
        List<String> scopes = addon.getScopeList();
        // a slot every signed-in user can open never carries orders, whatever the
        // browser sends
        Map<String, ?> withOrders = known.withOrders() ? withPatientOrders(context) : pageOnly(context);
        Map<String, Object> filtered = AddonCatalog.filterContext(withOrders, scopes);
        List<String> accessions = AddonCatalog.contextOrders(withOrders, scopes);
        long ttl = settings.tokenTtlSeconds();
        // without orders.read the add-on may not see lab numbers: they travel sealed
        String token = AddonToken.sign(tokenKey(),
                new AddonToken.Claims(addon.getId(), userId, scopes, accessions, Instant.now().getEpochSecond() + ttl),
                !scopes.contains(AddonCatalog.ORDERS_READ));
        return new IssuedToken(token, ttl, scopes, filtered);
    }

    @Override
    public AddonToken.Claims authenticate(String bearer) {
        if (bearer == null || bearer.isBlank()) {
            throw new IllegalArgumentException("token");
        }
        if (bearer.startsWith(AddonToken.PREFIX)) {
            AddonToken.Claims claims = AddonToken.verify(tokenKey(), bearer, Instant.now().getEpochSecond());
            Addon addon = addonDAO.get(claims.addonId()).filter(Addon::isEnabled)
                    .orElseThrow(() -> new IllegalArgumentException("token"));
            List<String> effective = claims.scopes().stream().filter(addon.getScopeList()::contains).toList();
            return new AddonToken.Claims(claims.addonId(), claims.userId(), effective, claims.accessions(),
                    claims.expiresAt());
        }
        if (bearer.startsWith(AddonRegistryServiceImpl.API_KEY_PREFIX)) {
            Addon addon = addonDAO.findByApiKeyHash(AddonSignature.sha256Hex(bearer.getBytes()))
                    .filter(Addon::isEnabled).orElseThrow(() -> new IllegalArgumentException("token"));
            return new AddonToken.Claims(addon.getId(), null, addon.getScopeList(), null, Long.MAX_VALUE);
        }
        throw new IllegalArgumentException("token");
    }

    private static Map<String, ?> pageOnly(Map<String, ?> context) {
        return context != null && context.get("page") != null ? Map.of("page", context.get("page")) : Map.of();
    }

    @Override
    public String opaqueId(String addonId, String value) {
        byte[] mac;
        try {
            Mac m = Mac.getInstance("HmacSHA256");
            m.init(new SecretKeySpec(tokenKey(), "HmacSHA256"));
            mac = m.doFinal((addonId + "\n" + value).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(mac, 16));
    }

    /**
     * A page that shows one patient (the patient results page) names the patient
     * instead of listing lab numbers; the server looks up that patient's orders,
     * newest first. The patient id itself is not passed on to the add-on, and a
     * page cannot add lab numbers of its own to a patient's list.
     */
    Map<String, ?> withPatientOrders(Map<String, ?> context) {
        if (context == null || !(context.get("patientId") instanceof Object raw)) {
            return context;
        }
        Map<String, Object> out = new LinkedHashMap<>(context);
        out.remove("patientId");
        String patientId = String.valueOf(raw);
        List<String> accessions = new ArrayList<>();
        if (patientId.matches("\\d{1,18}")) {
            sampleService.getSamplesForPatient(patientId).stream()
                    .sorted(Comparator.comparing(Sample::getReceivedTimestamp,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(Sample::getAccessionNumber).filter(AddonCatalog::isAccessionNumber).distinct()
                    .limit(AddonCatalog.MAX_CONTEXT_ACCESSIONS).forEach(accessions::add);
        }
        out.put("accessionNumbers", accessions);
        return out;
    }

    byte[] tokenKey() {
        byte[] key = tokenKey;
        if (key == null) {
            synchronized (this) {
                if (tokenKey == null) {
                    byte[] candidate = new byte[32];
                    new SecureRandom().nextBytes(candidate);
                    String stored = secretDAO.getOrCreate(TOKEN_KEY, Base64.getEncoder().encodeToString(candidate));
                    tokenKey = Base64.getDecoder().decode(stored);
                }
                key = tokenKey;
            }
        }
        return key;
    }
}
