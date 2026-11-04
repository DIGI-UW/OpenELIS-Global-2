package org.openelisglobal.addon.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.security.converter.EncryptionConverter;

/**
 * An installed add-on. {@code publisher} is the name the manifest claims;
 * {@code verifiedPublisher} is set only when a trusted key signed the package.
 * The manifest is kept verbatim; scopes and events are copied out of it so SQL
 * (the outbox trigger, the fan-out) can read them. The event secret is
 * encrypted at rest; the service API key is stored only as its SHA-256.
 */
@Entity
@Table(name = "addon", schema = "clinlims")
public class Addon extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 32)
    private String id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "version", nullable = false, length = 40)
    private String version;

    @Column(name = "publisher", nullable = false, length = 64)
    private String publisher;

    /** VERIFIED, UNVERIFIED or MISMATCH: see AddonSignature.Status. */
    @Column(name = "provenance", nullable = false, length = 16)
    private String provenance = "UNVERIFIED";

    /** The publisher whose trusted key signed the package; null unless VERIFIED. */
    @Column(name = "verified_publisher", length = 64)
    private String verifiedPublisher;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "manifest", nullable = false)
    private String manifest;

    @Column(name = "image", nullable = false, length = 255)
    private String image;

    @Column(name = "scopes", nullable = false, length = 400)
    private String scopes = "";

    @Column(name = "events", nullable = false, length = 400)
    private String events = "";

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "endpoint", length = 255)
    private String endpoint;

    @Column(name = "event_secret", nullable = false, length = 512)
    @Convert(converter = EncryptionConverter.class)
    private String eventSecret;

    @Column(name = "api_key_hash", nullable = false, length = 64)
    private String apiKeyHash;

    @Column(name = "config", nullable = false)
    private String config = "{}";

    /**
     * Values of {@code secret} configuration fields, a JSON object, encrypted at
     * rest.
     */
    @Column(name = "secret_config")
    @Convert(converter = EncryptionConverter.class)
    private String secretConfig;

    @Column(name = "installed_at", nullable = false)
    private OffsetDateTime installedAt;

    @Column(name = "installed_by", length = 64)
    private String installedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String provenance) {
        this.provenance = provenance;
    }

    public String getVerifiedPublisher() {
        return verifiedPublisher;
    }

    public void setVerifiedPublisher(String verifiedPublisher) {
        this.verifiedPublisher = verifiedPublisher;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManifest() {
        return manifest;
    }

    public void setManifest(String manifest) {
        this.manifest = manifest;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getScopeList() {
        return split(scopes);
    }

    public void setScopeList(List<String> list) {
        this.scopes = String.join(",", list);
    }

    public List<String> getEventList() {
        return split(events);
    }

    public void setEventList(List<String> list) {
        this.events = String.join(",", list);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEventSecret() {
        return eventSecret;
    }

    public void setEventSecret(String eventSecret) {
        this.eventSecret = eventSecret;
    }

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public void setApiKeyHash(String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public String getSecretConfig() {
        return secretConfig;
    }

    public void setSecretConfig(String secretConfig) {
        this.secretConfig = secretConfig;
    }

    public OffsetDateTime getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(OffsetDateTime installedAt) {
        this.installedAt = installedAt;
    }

    public String getInstalledBy() {
        return installedBy;
    }

    public void setInstalledBy(String installedBy) {
        this.installedBy = installedBy;
    }

    private static List<String> split(String csv) {
        List<String> out = new ArrayList<>();
        if (csv != null) {
            for (String s : csv.split(",")) {
                if (!s.isBlank()) {
                    out.add(s.trim());
                }
            }
        }
        return out;
    }
}
