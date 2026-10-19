package org.openelisglobal.addon.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.dao.AddonDAO;
import org.openelisglobal.addon.dao.AddonSecretDAO;
import org.openelisglobal.addon.packaging.AddonManifest;
import org.openelisglobal.addon.packaging.AddonSignature;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.valueholder.Addon;
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
        Map<String, Object> filtered = AddonCatalog.filterContext(context, scopes);
        @SuppressWarnings("unchecked")
        List<String> accessions = (List<String>) filtered.getOrDefault("accessionNumbers", List.of());
        long ttl = settings.tokenTtlSeconds();
        String token = AddonToken.sign(tokenKey(),
                new AddonToken.Claims(addon.getId(), userId, scopes, accessions, Instant.now().getEpochSecond() + ttl));
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
