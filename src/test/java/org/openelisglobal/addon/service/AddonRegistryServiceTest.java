package org.openelisglobal.addon.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.addon.AddonCatalog;
import org.openelisglobal.addon.AddonTestPackages;
import org.openelisglobal.addon.AddonTestPackages.Publisher;
import org.openelisglobal.addon.packaging.AddonPackageException;
import org.openelisglobal.addon.security.AddonToken;
import org.openelisglobal.addon.service.AddonRegistryService.InstallResult;
import org.openelisglobal.addon.service.AddonTokenService.Refusal;
import org.openelisglobal.addon.service.AddonTokenService.TokenRefusedException;
import org.openelisglobal.addon.valueholder.Addon;
import org.openelisglobal.security.converter.EncryptionConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The registry against a real database: signed install, upgrade rules, and what
 * turning an add-on off really stops (tokens, keys, queued deliveries, new
 * events, UI files).
 */
public class AddonRegistryServiceTest extends BaseWebContextSensitiveTest {

    private static final String ADMIN = "1";
    private static final Map<String, Object> CONTEXT = Map.of("page", "result-lookup", "accessionNumbers",
            List.of("DEV0001"));
    private static final Set<String> RESULTS_USER = Set.of("RESULTS");

    @Autowired
    private AddonRegistryService registry;
    @Autowired
    private AddonTokenService tokens;
    @Autowired
    private AddonSettings settings;

    private final Publisher example = new Publisher("example");
    private String originalTrust;

    @Before
    public void trustExample() {
        originalTrust = (String) ReflectionTestUtils.getField(settings, "trustedPublishers");
        ReflectionTestUtils.setField(settings, "trustedPublishers", example.trustSetting());
        cleanUp();
    }

    @After
    public void restore() {
        ReflectionTestUtils.setField(settings, "trustedPublishers", originalTrust);
        cleanUp();
    }

    private void cleanUp() {
        jdbcTemplate.update("DELETE FROM clinlims.addon_event");
        jdbcTemplate.update("DELETE FROM clinlims.addon WHERE id LIKE 'reg-%'");
    }

    private InstallResult install(String id) {
        return registry.install(AddonTestPackages.signedPackage(example, id), ADMIN);
    }

    private static byte[] unsigned(String id, String scopes, String events) {
        return AddonTestPackages
                .zip(AddonTestPackages.files(AddonTestPackages.manifest(id, "example", scopes, events)));
    }

    @Test
    public void anUnsignedPackageInstallsAsUnverified() {
        InstallResult result = registry.install(unsigned("reg-unsigned", "[\"orders.read\"]", "[\"order.created\"]"),
                ADMIN);
        assertEquals("UNVERIFIED", result.addon().provenance());
        assertNull(result.addon().verifiedPublisher());
        assertEquals("the claimed name is kept, but not as verified", "example", result.addon().publisher());
        assertFalse(result.addon().enabled());
    }

    @Test
    public void anInvalidManifestIsStillRefused() {
        AddonPackageException e = assertThrows(AddonPackageException.class,
                () -> registry.install(unsigned("Bad Id", "[]", "[]"), ADMIN));
        assertEquals("manifest.invalid", e.getCode());
        assertTrue(registry.get("Bad Id").isEmpty());
    }

    @Test
    public void withNoTrustedPublisherASignedPackageIsUnverified() {
        ReflectionTestUtils.setField(settings, "trustedPublishers", "");
        InstallResult result = install("reg-unknown");
        assertEquals("UNVERIFIED", result.addon().provenance());
        assertNull(result.addon().verifiedPublisher());
    }

    @Test
    public void aTamperedPackageInstallsButIsNeverShownAsVerified() {
        Map<String, byte[]> files = example.sign(AddonTestPackages.files(
                AddonTestPackages.manifest("reg-tamper", "example", "[\"orders.read\"]", "[\"order.created\"]")));
        files.put("ui/panel.js", "somethingElse()".getBytes());
        InstallResult result = registry.install(AddonTestPackages.zip(files), ADMIN);
        assertEquals("MISMATCH", result.addon().provenance());
        assertNull(result.addon().verifiedPublisher());
        assertFalse(result.addon().enabled());
        assertEquals("MISMATCH", registry.get("reg-tamper").orElseThrow().provenance());
    }

    @Test
    public void aTrustedKeySigningAManifestThatClaimsAnotherPublisherIsAMismatch() {
        Publisher other = new Publisher("other");
        ReflectionTestUtils.setField(settings, "trustedPublishers",
                example.trustSetting() + "," + other.trustSetting());
        // signed by "other", but addon.json says "example"
        byte[] zip = AddonTestPackages.zip(other.sign(AddonTestPackages.files(
                AddonTestPackages.manifest("reg-claim", "example", "[\"orders.read\"]", "[\"order.created\"]"))));
        InstallResult result = registry.install(zip, ADMIN);
        assertEquals("MISMATCH", result.addon().provenance());
        assertNull(result.addon().verifiedPublisher());
    }

    @Test
    public void aNewAddonIsRegisteredDisabledWithCredentialsAndAComposeSnippet() {
        InstallResult result = install("reg-new");
        assertFalse(result.upgraded());
        assertEquals("VERIFIED", result.addon().provenance());
        assertEquals("example", result.addon().verifiedPublisher());
        assertFalse(result.addon().enabled());
        assertEquals(List.of(AddonCatalog.ORDERS_READ, AddonCatalog.RESULTS_READ), result.addon().scopes());
        assertEquals("http://reg-new:8080/events", result.addon().endpoint());
        assertEquals(Map.of("greeting", "Hi", "limit", 5.0), result.addon().config());
        assertTrue(result.credentials().apiKey().startsWith("oeak_"));
        assertTrue(result.credentials().webhookSecret().startsWith("whsec_"));
        assertTrue(result.composeSnippet().contains("image: " + AddonTestPackages.IMAGE));
        assertTrue(result.composeSnippet().contains("OPENELIS_API_KEY: " + result.credentials().apiKey()));
        // the key is stored hashed, never in clear
        String stored = jdbcTemplate.queryForObject("SELECT api_key_hash FROM clinlims.addon WHERE id = 'reg-new'",
                String.class);
        assertFalse(stored.contains(result.credentials().apiKey()));
    }

    @Test
    public void aDisabledAddonGetsNoTokenAndItsFilesAreNotServed() {
        install("reg-off");
        TokenRefusedException e = assertThrows(TokenRefusedException.class, () -> tokens.issue("reg-off",
                AddonCatalog.SLOT_RESULT_LOOKUP_SIDE_PANEL, CONTEXT, ADMIN, RESULTS_USER));
        assertEquals(Refusal.NOT_FOUND, e.getRefusal());
        assertTrue(registry.asset("reg-off", "panel.html").isEmpty());
        assertTrue(registry.activeExtensions().stream().noneMatch(x -> x.addonId().equals("reg-off")));

        registry.setEnabled("reg-off", true);
        assertTrue(registry.asset("reg-off", "panel.html").isPresent());
        assertEquals("text/html; charset=utf-8", registry.asset("reg-off", "panel.html").get().getContentType());
        assertTrue(registry.activeExtensions().stream().anyMatch(
                x -> x.addonId().equals("reg-off") && x.slot().equals(AddonCatalog.SLOT_RESULT_LOOKUP_SIDE_PANEL)));
    }

    @Test
    public void disablingRevokesTokensAndTheServiceKeyAtOnce() {
        String apiKey = install("reg-revoke").credentials().apiKey();
        registry.setEnabled("reg-revoke", true);
        String token = tokens
                .issue("reg-revoke", AddonCatalog.SLOT_RESULT_LOOKUP_SIDE_PANEL, CONTEXT, ADMIN, RESULTS_USER).token();
        assertEquals("reg-revoke", tokens.authenticate(token).addonId());
        assertTrue(tokens.authenticate(apiKey).isService());

        registry.setEnabled("reg-revoke", false);
        assertThrows(IllegalArgumentException.class, () -> tokens.authenticate(token));
        assertThrows(IllegalArgumentException.class, () -> tokens.authenticate(apiKey));
    }

    @Test
    public void theTokenCarriesOnlyTheFilteredContext() {
        install("reg-ctx");
        registry.setEnabled("reg-ctx", true);
        Map<String, Object> context = Map.of("page", "result-lookup", "accessionNumbers", List.of("DEV0001"),
                "patientName", "Jane Doe");
        AddonTokenService.IssuedToken issued = tokens.issue("reg-ctx", AddonCatalog.SLOT_RESULT_LOOKUP_SIDE_PANEL,
                context, ADMIN, RESULTS_USER);
        assertEquals(Map.of("page", "result-lookup", "accessionNumbers", List.of("DEV0001")), issued.context());
        AddonToken.Claims claims = tokens.authenticate(issued.token());
        assertEquals(List.of("DEV0001"), claims.accessions());
        assertEquals(ADMIN, claims.userId());
    }

    @Test
    public void aTokenIsRefusedForASlotTheAddonDoesNotUseOrARoleTheSlotNeeds() {
        install("reg-slot");
        registry.setEnabled("reg-slot", true);
        assertEquals(Refusal.SLOT,
                assertThrows(TokenRefusedException.class, () -> tokens.issue("reg-slot",
                        AddonCatalog.SLOT_RESULT_VALIDATION_SIDE_PANEL, CONTEXT, ADMIN, Set.of("VALIDATION")))
                        .getRefusal());
        assertEquals(Refusal.ROLE, assertThrows(TokenRefusedException.class, () -> tokens.issue("reg-slot",
                AddonCatalog.SLOT_RESULT_LOOKUP_SIDE_PANEL, CONTEXT, ADMIN, Set.of("BILLING"))).getRefusal());
    }

    @Test
    public void rotatingCredentialsInvalidatesTheOldKey() {
        String oldKey = install("reg-rotate").credentials().apiKey();
        registry.setEnabled("reg-rotate", true);
        String newKey = registry.rotateCredentials("reg-rotate").orElseThrow().apiKey();
        assertThrows(IllegalArgumentException.class, () -> tokens.authenticate(oldKey));
        assertEquals("reg-rotate", tokens.authenticate(newKey).addonId());
    }

    @Test
    public void anUpgradeAskingForMoreIsTurnedOffAndKeepsItsCredentials() {
        String key = install("reg-up").credentials().apiKey();
        registry.setEnabled("reg-up", true);

        // same permissions, new version: stays on, no new credentials
        byte[] same = AddonTestPackages.zip(example.sign(AddonTestPackages.files(AddonTestPackages
                .manifest("reg-up", "example", "[\"orders.read\",\"results.read\"]", "[\"result.validated\"]")
                .replace("1.0.0", "1.1.0"))));
        InstallResult sameResult = registry.install(same, ADMIN);
        assertTrue(sameResult.upgraded());
        assertNull(sameResult.credentials());
        assertTrue(sameResult.addon().enabled());
        assertEquals("1.1.0", sameResult.addon().version());

        // asks for patient.read: off until an administrator looks again
        byte[] more = AddonTestPackages.zip(example.sign(AddonTestPackages.files(AddonTestPackages.manifest("reg-up",
                "example", "[\"orders.read\",\"results.read\",\"patient.read\"]", "[\"result.validated\"]"))));
        assertFalse(registry.install(more, ADMIN).addon().enabled());
        registry.setEnabled("reg-up", true);
        assertEquals("reg-up", tokens.authenticate(key).addonId());
    }

    @Test
    public void aVerifiedAddonReplacedByAWeakerPackageIsTurnedOff() {
        install("reg-owner");
        registry.setEnabled("reg-owner", true);

        // same permissions, but unsigned: installed, off until reviewed, no longer
        // verified
        InstallResult unsignedResult = registry.install(
                unsigned("reg-owner", "[\"orders.read\",\"results.read\"]", "[\"result.validated\",\"order.created\"]"),
                ADMIN);
        assertFalse(unsignedResult.addon().enabled());
        assertEquals("UNVERIFIED", unsignedResult.addon().provenance());
        assertNull(unsignedResult.addon().verifiedPublisher());

        // verified by another trusted publisher: also off until reviewed
        install("reg-owner");
        registry.setEnabled("reg-owner", true);
        Publisher other = new Publisher("other");
        ReflectionTestUtils.setField(settings, "trustedPublishers",
                example.trustSetting() + "," + other.trustSetting());
        InstallResult otherResult = registry.install(AddonTestPackages.signedPackage(other, "reg-owner"), ADMIN);
        assertFalse(otherResult.addon().enabled());
        assertEquals("other", otherResult.addon().verifiedPublisher());
    }

    @Test
    public void anUnverifiedAddonUpgradedWithTheSamePermissionsStaysOn() {
        String scopes = "[\"orders.read\"]";
        String events = "[\"order.created\"]";
        registry.install(unsigned("reg-dev", scopes, events), ADMIN);
        registry.setEnabled("reg-dev", true);
        assertTrue(registry.install(unsigned("reg-dev", scopes, events), ADMIN).addon().enabled());
    }

    @Test
    public void configurationIsValidatedAndServedToTheAddon() {
        install("reg-cfg");
        assertEquals(Map.of("greeting", "Hello", "limit", 9.0),
                registry.saveConfig("reg-cfg", Map.of("greeting", "Hello", "limit", 9)).orElseThrow());
        assertEquals(Map.of("greeting", "Hello", "limit", 9.0), registry.configOf("reg-cfg"));
        assertThrows(IllegalArgumentException.class, () -> registry.saveConfig("reg-cfg", Map.of("limit", 0)));
        assertThrows(IllegalArgumentException.class, () -> registry.setEndpoint("reg-cfg", "file:///etc/passwd"));
        assertTrue(registry.setEndpoint("reg-cfg", "https://hello.lab.internal/events"));
    }

    @Test
    public void uninstallRemovesTheAddonAndItsFiles() {
        install("reg-gone");
        assertTrue(registry.uninstall("reg-gone"));
        assertTrue(registry.get("reg-gone").isEmpty());
        assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                "SELECT count(*) FROM clinlims.addon_asset WHERE addon_id = 'reg-gone'", Integer.class));
        assertFalse(registry.uninstall("reg-gone"));
        assertNotNull(registry.list());
    }

    private static Class<?> converterOf(String field) {
        try {
            return Addon.class.getDeclaredField(field).getAnnotation(jakarta.persistence.Convert.class).converter();
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] withEgress(String id, String egressJson) {
        return AddonTestPackages.zip(AddonTestPackages.files(AddonTestPackages.withEgressAndSecret(
                AddonTestPackages.manifest(id, "example", "[\"orders.read\"]", "[\"order.created\"]"), egressJson)));
    }

    @Test
    public void secretsAreStoredEncryptedNeverShownAndServedToTheAddon() {
        registry.install(withEgress("reg-secret", "[\"openrouter.ai\"]"), ADMIN);
        registry.saveConfig("reg-secret", Map.of("greeting", "Hi", "limit", 5, "apiKey", "sk-or-very-secret"));
        AddonRegistryService.AddonView view = registry.get("reg-secret").orElseThrow();
        assertFalse(view.config().containsKey("apiKey"));
        assertEquals(List.of("apiKey"), view.secretsSet());
        assertEquals(List.of("openrouter.ai"), view.egress());
        assertEquals("sk-or-very-secret", registry.configOf("reg-secret").get("apiKey"));
        // encrypted at rest with EncryptionConverter, like the webhook secret (the test
        // context's encryptor is a stub)
        assertEquals(EncryptionConverter.class, converterOf("secretConfig"));
        assertEquals(converterOf("eventSecret"), converterOf("secretConfig"));

        // absent: kept; empty: cleared
        registry.saveConfig("reg-secret", Map.of("greeting", "Hello"));
        assertEquals("sk-or-very-secret", registry.configOf("reg-secret").get("apiKey"));
        registry.saveConfig("reg-secret", Map.of("apiKey", ""));
        assertFalse(registry.configOf("reg-secret").containsKey("apiKey"));
        assertEquals(List.of(), registry.get("reg-secret").orElseThrow().secretsSet());
    }

    @Test
    public void anUpgradeThatConnectsToMoreHostsIsTurnedOff() {
        registry.install(withEgress("reg-net", "[\"openrouter.ai\"]"), ADMIN);
        registry.setEnabled("reg-net", true);
        assertTrue("same hosts: stays on",
                registry.install(withEgress("reg-net", "[\"openrouter.ai\"]"), ADMIN).addon().enabled());
        InstallResult more = registry.install(withEgress("reg-net", "[\"openrouter.ai\",\"api.deepseek.com\"]"), ADMIN);
        assertFalse(more.addon().enabled());
        registry.setEnabled("reg-net", true);
        assertTrue("fewer hosts: stays on", registry.install(withEgress("reg-net", "[]"), ADMIN).addon().enabled());
    }

    @Test
    public void theComposeSnippetSaysWhereTheAddonConnects() {
        assertTrue(registry.install(withEgress("reg-snip", "[\"openrouter.ai\"]"), ADMIN).composeSnippet()
                .contains("# network: connects to openrouter.ai"));
        assertTrue(install("reg-snip2").composeSnippet().contains("needs nothing outside OpenELIS"));
    }

    @Test
    public void anAiContextTokenCarriesItsOrdersSealed() {
        registry.install(AddonTestPackages.zip(
                AddonTestPackages.files(AddonTestPackages.manifest("reg-ai", "example", "[\"ai.context\"]", "[]"))),
                ADMIN);
        registry.setEnabled("reg-ai", true);
        AddonTokenService.IssuedToken issued = tokens.issue("reg-ai", AddonCatalog.SLOT_RESULT_LOOKUP_SIDE_PANEL,
                CONTEXT, ADMIN, RESULTS_USER);
        assertFalse("the frame does not get the lab numbers", issued.context().containsKey("accessionNumbers"));
        assertFalse(issued.token(),
                new String(java.util.Base64.getUrlDecoder()
                        .decode(issued.token().substring(AddonToken.PREFIX.length(), issued.token().lastIndexOf('.'))))
                        .contains("DEV0001"));
        assertEquals(List.of("DEV0001"), tokens.authenticate(issued.token()).accessions());
    }
}
