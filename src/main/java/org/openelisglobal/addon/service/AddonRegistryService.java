package org.openelisglobal.addon.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.addon.packaging.AddonManifest;
import org.openelisglobal.addon.valueholder.AddonAsset;

/**
 * The add-on registry: install (register) a package, list, enable or disable,
 * configure, remove. Running the add-on's container is the operator's job;
 * installing returns the credentials and a docker-compose snippet for it.
 */
public interface AddonRegistryService {

    /**
     * Secrets the add-on's container needs. Returned once; only hashes or
     * ciphertext are kept.
     */
    record Credentials(String apiKey, String webhookSecret) {
    }

    /**
     * Result of an install. {@code credentials} and {@code composeSnippet} are only
     * present for a new add-on (an upgrade keeps its credentials).
     */
    record InstallResult(AddonView addon, boolean upgraded, Credentials credentials, String composeSnippet) {
    }

    /**
     * One add-on as the administration page shows it. {@code publisher}: the name
     * the manifest claims. {@code provenance}: VERIFIED, UNVERIFIED or MISMATCH;
     * {@code verifiedPublisher} is set only when VERIFIED. The UI must show the
     * claimed name as unverified otherwise. {@code config} holds no secret:
     * {@code secretsSet} names the secret fields that have a value. {@code egress}:
     * hosts outside OpenELIS the add-on says it connects to.
     */
    record AddonView(String id, String name, String version, String publisher, String provenance,
            String verifiedPublisher, String description, String image, boolean enabled, String endpoint,
            List<String> scopes, List<String> events, List<AddonManifest.Extension> extensions,
            List<AddonManifest.ConfigField> configSchema, Map<String, Object> config, List<String> secretsSet,
            List<String> egress, String installedAt, String installedBy, Map<String, Integer> deliveries) {
    }

    /** A UI extension of an enabled add-on, for the frontend's slots. */
    record ExtensionView(String addonId, String addonName, String version, String name, String slot, String page,
            int order, String title, int height, String icon) {
    }

    /**
     * Validates the manifest, records who published the package (the signature only
     * establishes provenance; unsigned packages install too), and registers or
     * upgrades the add-on. A new add-on is registered disabled: turning it on is
     * the administrator's consent to its permissions. An upgrade is turned off as
     * well when it asks for more scopes, events or outside hosts, or when its
     * provenance is weaker than the installed version's (a verified add-on replaced
     * by an unverified or mismatched package).
     *
     * @throws org.openelisglobal.addon.packaging.AddonPackageException when the zip
     *                                                                  or the
     *                                                                  manifest is
     *                                                                  invalid
     */
    InstallResult install(byte[] packageZip, String sysUserId);

    List<AddonView> list();

    Optional<AddonView> get(String id);

    /**
     * Turning an add-on off takes effect at once: its pending deliveries are
     * dropped, no new event is queued for it, and its tokens and API key are
     * refused from the next request.
     *
     * @return false if there is no such add-on
     */
    boolean setEnabled(String id, boolean enabled);

    /**
     * Saves the configuration. Secret fields: a non-empty value sets the secret, an
     * empty string clears it, an absent key keeps it.
     *
     * @return the plain configuration (never the secrets)
     * @throws IllegalArgumentException naming a bad key
     */
    Optional<Map<String, Object>> saveConfig(String id, Map<String, ?> values);

    /** @throws IllegalArgumentException if the URL is not http(s) */
    boolean setEndpoint(String id, String endpoint);

    Optional<Credentials> rotateCredentials(String id);

    boolean uninstall(String id);

    List<ExtensionView> activeExtensions();

    /** A UI file of an enabled add-on. */
    Optional<AddonAsset> asset(String addonId, String path);

    List<Map<String, Object>> deliveries(String id, int limit);

    /**
     * Configuration values including secrets, as the add-on API serves them to the
     * add-on itself.
     */
    Map<String, Object> configOf(String id);
}
