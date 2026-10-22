package org.openelisglobal.addon.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.addon.dao.AddonAssetDAO;
import org.openelisglobal.addon.dao.AddonDAO;
import org.openelisglobal.addon.dao.AddonOutboxDAO;
import org.openelisglobal.addon.packaging.AddonManifest;
import org.openelisglobal.addon.packaging.AddonPackageReader;
import org.openelisglobal.addon.packaging.AddonSignature;
import org.openelisglobal.addon.security.WebhookSignature;
import org.openelisglobal.addon.valueholder.Addon;
import org.openelisglobal.addon.valueholder.AddonAsset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddonRegistryServiceImpl implements AddonRegistryService {

    static final String API_KEY_PREFIX = "oeak_";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private AddonDAO addonDAO;
    @Autowired
    private AddonAssetDAO assetDAO;
    @Autowired
    private AddonOutboxDAO outboxDAO;
    @Autowired
    private AddonSettings settings;

    @Override
    public InstallResult install(byte[] packageZip, String sysUserId) {
        Map<String, byte[]> files = AddonPackageReader.read(packageZip);
        AddonManifest manifest = AddonManifest.fromPackage(files);
        AddonSignature.Provenance provenance = AddonSignature.check(files, settings.trustedPublishers());
        if (provenance.status() == AddonSignature.Status.VERIFIED
                && !manifest.publisher().equals(provenance.verifiedPublisher())) {
            // signed by a trusted key, but the manifest claims someone else
            provenance = AddonSignature.Provenance.mismatch();
        }

        Addon addon = addonDAO.get(manifest.id()).orElse(null);
        boolean upgrade = addon != null;
        Credentials credentials = null;
        if (upgrade) {
            List<String> oldEgress = AddonManifest.parse(addon.getManifest()).egress();
            boolean asksForMore = !new HashSet<>(addon.getScopeList()).containsAll(manifest.scopes())
                    || !new HashSet<>(addon.getEventList()).containsAll(manifest.events())
                    || !new HashSet<>(oldEgress).containsAll(manifest.egress());
            if ((asksForMore || weakerProvenance(addon, provenance)) && addon.isEnabled()) {
                addon.setEnabled(false);
                outboxDAO.dropPending(addon.getId());
            }
            addon.setConfig(carryOverConfig(manifest, addon.getConfig()));
            Map<String, Object> secrets = new LinkedHashMap<>(parseObject(addon.getSecretConfig()));
            secrets.keySet().retainAll(manifest.secretKeys());
            addon.setSecretConfig(secrets.isEmpty() ? null : toJson(secrets));
        } else {
            addon = new Addon();
            addon.setId(manifest.id());
            addon.setEnabled(false);
            addon.setEndpoint("http://" + manifest.id() + ":" + manifest.port() + manifest.eventsPath());
            addon.setInstalledAt(OffsetDateTime.now());
            addon.setInstalledBy(sysUserId);
            credentials = newCredentials();
            addon.setEventSecret(credentials.webhookSecret());
            addon.setApiKeyHash(AddonSignature.sha256Hex(credentials.apiKey().getBytes()));
            addon.setConfig(toJson(defaultConfig(manifest)));
        }
        addon.setName(manifest.name());
        addon.setVersion(manifest.version());
        addon.setPublisher(manifest.publisher());
        addon.setProvenance(provenance.status().name());
        addon.setVerifiedPublisher(provenance.verifiedPublisher());
        addon.setDescription(manifest.description());
        addon.setManifest(manifest.raw());
        addon.setImage(manifest.image());
        addon.setScopeList(manifest.scopes());
        addon.setEventList(manifest.events());
        addon.setSysUserId(sysUserId);
        addon = addonDAO.save(addon);

        assetDAO.deleteAll(addon.getId());
        for (Map.Entry<String, byte[]> file : AddonManifest.uiFiles(files).entrySet()) {
            AddonAsset asset = new AddonAsset();
            asset.setAddonId(addon.getId());
            asset.setPath(file.getKey());
            asset.setContentType(AddonManifest.contentType(file.getKey()));
            asset.setContent(file.getValue());
            asset.setSysUserId(sysUserId);
            assetDAO.save(asset);
        }
        return new InstallResult(view(addon), upgrade, credentials,
                credentials == null ? null : composeSnippet(manifest, credentials));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddonView> list() {
        List<AddonView> out = new ArrayList<>();
        for (Addon addon : addonDAO.findAllOrdered()) {
            out.add(view(addon));
        }
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AddonView> get(String id) {
        return addonDAO.get(id).map(this::view);
    }

    @Override
    public boolean setEnabled(String id, boolean enabled) {
        Optional<Addon> found = addonDAO.get(id);
        if (found.isEmpty()) {
            return false;
        }
        Addon addon = found.get();
        addon.setEnabled(enabled);
        addonDAO.save(addon);
        if (!enabled) {
            outboxDAO.dropPending(id);
        }
        return true;
    }

    @Override
    public Optional<Map<String, Object>> saveConfig(String id, Map<String, ?> values) {
        Optional<Addon> found = addonDAO.get(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Addon addon = found.get();
        AddonManifest manifest = AddonManifest.parse(addon.getManifest());
        Map<String, Object> clean = manifest.validateConfig(values);
        Map<String, Object> secrets = new LinkedHashMap<>(parseObject(addon.getSecretConfig()));
        manifest.validateSecrets(values).forEach((k, v) -> {
            if (v.isEmpty()) {
                secrets.remove(k);
            } else {
                secrets.put(k, v);
            }
        });
        secrets.keySet().retainAll(manifest.secretKeys());
        addon.setConfig(toJson(clean));
        addon.setSecretConfig(secrets.isEmpty() ? null : toJson(secrets));
        addonDAO.save(addon);
        return Optional.of(clean);
    }

    @Override
    public boolean setEndpoint(String id, String endpoint) {
        URI uri;
        try {
            uri = new URI(endpoint == null ? "" : endpoint.trim());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("endpoint");
        }
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null
                || uri.toString().length() > 255) {
            throw new IllegalArgumentException("endpoint");
        }
        Optional<Addon> found = addonDAO.get(id);
        if (found.isEmpty()) {
            return false;
        }
        found.get().setEndpoint(uri.toString());
        addonDAO.save(found.get());
        return true;
    }

    @Override
    public Optional<Credentials> rotateCredentials(String id) {
        Optional<Addon> found = addonDAO.get(id);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Credentials credentials = newCredentials();
        Addon addon = found.get();
        addon.setEventSecret(credentials.webhookSecret());
        addon.setApiKeyHash(AddonSignature.sha256Hex(credentials.apiKey().getBytes()));
        addonDAO.save(addon);
        return Optional.of(credentials);
    }

    @Override
    public boolean uninstall(String id) {
        Optional<Addon> found = addonDAO.get(id);
        if (found.isEmpty()) {
            return false;
        }
        // assets and deliveries go with the row (ON DELETE CASCADE)
        addonDAO.remove(found.get());
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtensionView> activeExtensions() {
        List<ExtensionView> out = new ArrayList<>();
        for (Addon addon : addonDAO.findAllOrdered()) {
            if (!addon.isEnabled()) {
                continue;
            }
            for (AddonManifest.Extension e : AddonManifest.parse(addon.getManifest()).extensions()) {
                out.add(new ExtensionView(addon.getId(), addon.getName(), addon.getVersion(), e.name(), e.slot(),
                        e.page(), e.order(), e.title(), e.height(), e.icon(), e.titles()));
            }
        }
        out.sort(Comparator.comparing(ExtensionView::slot).thenComparingInt(ExtensionView::order)
                .thenComparing(ExtensionView::addonId).thenComparing(ExtensionView::name));
        return out;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AddonAsset> asset(String addonId, String path) {
        Optional<Addon> addon = addonDAO.get(addonId);
        if (addon.isEmpty() || !addon.get().isEnabled()) {
            return Optional.empty();
        }
        return assetDAO.find(addonId, path);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> deliveries(String id, int limit) {
        return outboxDAO.recent(id, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> configOf(String id) {
        return addonDAO.get(id).map(a -> {
            Map<String, Object> all = new LinkedHashMap<>(parseObject(a.getConfig()));
            all.putAll(parseObject(a.getSecretConfig()));
            return (Map<String, Object>) all;
        }).orElse(Map.of());
    }

    // ----- helpers

    private AddonView view(Addon addon) {
        AddonManifest manifest = AddonManifest.parse(addon.getManifest());
        return new AddonView(addon.getId(), addon.getName(), addon.getVersion(), addon.getPublisher(),
                addon.getProvenance(), addon.getVerifiedPublisher(), addon.getDescription(), addon.getImage(),
                addon.isEnabled(), addon.getEndpoint(), addon.getScopeList(), addon.getEventList(),
                manifest.extensions(), manifest.config(), parseObject(addon.getConfig()),
                new ArrayList<>(parseObject(addon.getSecretConfig()).keySet()), manifest.egress(),
                addon.getInstalledAt() == null ? null : addon.getInstalledAt().toString(), addon.getInstalledBy(),
                outboxDAO.countsByStatus(addon.getId()));
    }

    /**
     * A verified add-on may only be replaced, without a new review, by a package
     * verified for the same publisher; a mismatched package is always reviewed.
     */
    static boolean weakerProvenance(Addon installed, AddonSignature.Provenance incoming) {
        if (incoming.status() == AddonSignature.Status.MISMATCH) {
            return true;
        }
        if (AddonSignature.Status.VERIFIED.name().equals(installed.getProvenance())) {
            return incoming.status() != AddonSignature.Status.VERIFIED
                    || !incoming.verifiedPublisher().equals(installed.getVerifiedPublisher());
        }
        return false;
    }

    static Credentials newCredentials() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        return new Credentials(API_KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(key),
                WebhookSignature.newSecret());
    }

    private static Map<String, Object> defaultConfig(AddonManifest manifest) {
        try {
            return manifest.validateConfig(Map.of());
        } catch (IllegalArgumentException e) {
            return Map.of(); // a required value without default: the administrator sets it
        }
    }

    private static String carryOverConfig(AddonManifest manifest, String oldJson) {
        Map<String, Object> old = new LinkedHashMap<>(parseObject(oldJson));
        old.keySet().removeIf(k -> manifest.config().stream().noneMatch(f -> f.key().equals(k)));
        try {
            return toJson(manifest.validateConfig(old));
        } catch (IllegalArgumentException e) {
            return toJson(defaultConfig(manifest));
        }
    }

    static String composeSnippet(AddonManifest manifest, Credentials credentials) {
        return String.join("\n", "services:", "  " + manifest.id() + ":", "    image: " + manifest.image(),
                "    restart: unless-stopped", "    read_only: true", "    cap_drop: [ALL]",
                "    security_opt: [\"no-new-privileges:true\"]", "    user: \"65534:65534\"", "    mem_limit: 256m",
                "    environment:",
                "      OPENELIS_API_URL: https://oe.openelis.org:8443/api/OpenELIS-Global/rest/addon-api/v1",
                "      OPENELIS_API_KEY: " + credentials.apiKey(),
                "      OPENELIS_WEBHOOK_SECRET: " + credentials.webhookSecret(), "    networks:", "      - default",
                egressNote(manifest.egress()), "");
    }

    /**
     * What the add-on says about the network, for the operator's network policy.
     */
    static String egressNote(List<String> egress) {
        if (egress.isEmpty()) {
            return "    # network: needs nothing outside OpenELIS; an internal network is enough";
        }
        return "    # network: connects to " + String.join(", ", egress)
                + " (allow only these, for example through an allowlisting proxy)";
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseObject(String json) {
        try {
            Map<String, Object> m = JSON.readValue(json == null ? "{}" : json, Map.class);
            return m == null ? Map.of() : m;
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    static String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
