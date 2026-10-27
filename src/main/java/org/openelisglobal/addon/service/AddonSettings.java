package org.openelisglobal.addon.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openelisglobal.addon.packaging.AddonSignature;
import org.openelisglobal.common.log.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Server settings of the add-on mechanism (common.properties or
 * extra.properties):
 *
 * <ul>
 * <li>{@code addons.trustedPublishers}: {@code name=base64key[,…]}, the
 * publishers whose signed packages show as verified. It never blocks an
 * install. Empty (the default): every package shows as unverified.</li>
 * <li>{@code addons.api.allowedNetworks}: CIDR ranges allowed to call the
 * add-on API (default: private and loopback ranges, where add-on containers
 * run).</li>
 * <li>{@code addons.token.ttlSeconds} (600),
 * {@code addons.events.retentionDays} (7), {@code addons.dispatcher.enabled}
 * (true).</li>
 * <li>{@code addons.accessLog.retentionDays} (180): how long the access log of
 * the add-on API is kept. Local rules on audit trails may ask for longer.</li>
 * </ul>
 */
@Component
public class AddonSettings {

    @Value("${addons.trustedPublishers:}")
    private String trustedPublishers;

    @Value("${addons.api.allowedNetworks:10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,127.0.0.0/8,::1/128,fc00::/7}")
    private String allowedNetworks;

    @Value("${addons.token.ttlSeconds:600}")
    private long tokenTtlSeconds;

    @Value("${addons.events.retentionDays:7}")
    private int eventRetentionDays;

    @Value("${addons.dispatcher.enabled:true}")
    private boolean dispatcherEnabled;

    @Value("${addons.accessLog.retentionDays:180}")
    private int accessLogRetentionDays;

    /**
     * Publisher name → raw Ed25519 public key. A malformed setting verifies nobody.
     */
    public Map<String, byte[]> trustedPublishers() {
        try {
            return AddonSignature.parseTrustedPublishers(trustedPublishers);
        } catch (IllegalArgumentException e) {
            LogEvent.logError(getClass().getSimpleName(), "trustedPublishers", e.getMessage());
            return Map.of();
        }
    }

    public List<String> allowedNetworks() {
        List<String> out = new ArrayList<>();
        for (String s : allowedNetworks.split(",")) {
            if (!s.isBlank()) {
                out.add(s.trim());
            }
        }
        return out;
    }

    public long tokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public int eventRetentionDays() {
        return eventRetentionDays;
    }

    public boolean dispatcherEnabled() {
        return dispatcherEnabled;
    }

    public int accessLogRetentionDays() {
        return Math.max(1, accessLogRetentionDays);
    }
}
