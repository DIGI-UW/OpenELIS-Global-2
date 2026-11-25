package org.openelisglobal.internationalization;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The patient report reads its labels from the interface's catalogue, layered
 * as the browser layers it, so the report prints the screens' words in every
 * language they ship and any rewording the deployment mounts.
 */
public class UiMessagesTest {

    @Rule
    public TemporaryFolder overrides = new TemporaryFolder();

    @Test
    public void readsTheScreensCatalogueInTheUsersLanguage() {
        assertEquals("Result", UiMessages.forLocale(Locale.ENGLISH, null).get("label.results.result"));
        assertEquals("Analyse", UiMessages.forLocale(Locale.FRENCH, null).get("label.results.test"));
        assertEquals("Pemeriksaan", UiMessages.forLocale(Locale.of("id"), null).get("label.results.test"));
    }

    @Test
    public void aRegionalLanguageLayersOverItsBaseLanguageThenEnglish() throws Exception {
        UiMessages malagasyFrench = UiMessages.forLocale(Locale.of("fr", "MG"), null);
        Map<String, String> english = shippedCatalogue("en");
        Set<String> french = new HashSet<>(shippedCatalogue("fr").keySet());
        french.addAll(shippedCatalogue("fr_MG").keySet());
        String englishOnly = english.keySet().stream().filter(key -> !french.contains(key)).sorted().findFirst()
                .orElse(null);

        assertEquals("the regional file wins", "Suivante", malagasyFrench.get("reporting.next"));
        Assume.assumeNotNull(englishOnly);
        assertEquals("a key only English holds prints in English", english.get(englishOnly),
                malagasyFrench.get(englishOnly));
        assertEquals(List.of("fr", "fr_MG"), UiMessages.filesFor(Locale.of("fr", "MG")));
    }

    /**
     * A shipped file as it is, without the English beneath it. Transifex fills in
     * French for more keys with every sync, so a key named here would stop being
     * English-only.
     */
    private static Map<String, String> shippedCatalogue(String file) throws Exception {
        try (InputStream in = UiMessages.class.getClassLoader()
                .getResourceAsStream(UiMessages.CATALOGUE + file + ".json")) {
            Map<String, String> strings = new HashMap<>();
            new ObjectMapper().readTree(in).fields().forEachRemaining(entry -> {
                if (entry.getValue().isTextual()) {
                    strings.put(entry.getKey(), entry.getValue().asText());
                }
            });
            return strings;
        }
    }

    @Test
    public void aKeyNoLayerHoldsPrintsAsTheKey() {
        assertEquals("no.such.key", UiMessages.forLocale(Locale.ENGLISH, null).get("no.such.key"));
    }

    @Test
    public void theDeploymentsOverridesWinBaseLanguageFirstThenRegional() throws Exception {
        Path directory = overrides.getRoot().toPath();
        Files.writeString(directory.resolve("fr.json"),
                "{\"label.results.test\": \"Examen\", \"reporting.next\": \"Après\", \"nested\": {\"a\": \"b\"}}");
        Files.writeString(directory.resolve("fr_MG.json"), "{\"reporting.next\": \"Manaraka\"}");

        UiMessages french = UiMessages.forLocale(Locale.FRENCH, directory);
        UiMessages malagasyFrench = UiMessages.forLocale(Locale.of("fr", "MG"), directory);

        assertEquals("Examen", french.get("label.results.test"));
        assertEquals("Examen", malagasyFrench.get("label.results.test"));
        assertEquals("Manaraka", malagasyFrench.get("reporting.next"));
        assertEquals("a value that is not a string is ignored", "nested", french.get("nested"));
    }

    @Test
    public void overridesApplyOnlyWhenTheSiteMountsThem() throws Exception {
        Files.writeString(overrides.getRoot().toPath().resolve("en.json"), "{\"label.results.result\": \"Outcome\"}");

        assertEquals("Outcome",
                UiMessages.forLocale(Locale.ENGLISH, overrides.getRoot().toPath()).get("label.results.result"));
        assertEquals("Result", UiMessages.forLocale(Locale.ENGLISH, null).get("label.results.result"));
    }

    @Test
    public void aBrokenOverrideFileCostsOnlyItself() throws Exception {
        Files.writeString(overrides.getRoot().toPath().resolve("en.json"), "{ not json");

        assertEquals("Result",
                UiMessages.forLocale(Locale.ENGLISH, overrides.getRoot().toPath()).get("label.results.result"));
    }

    @Test
    public void fillsNumberedPlaceholdersAndKeepsApostrophes() {
        UiMessages english = UiMessages.forLocale(Locale.ENGLISH, null);

        assertEquals("Collected 01/10/26 08:10 (first of 3 samples)",
                english.format("report.patient.strip.collectedFirst", "01/10/26 08:10", 3));
        assertEquals("Not within the laboratory's accredited scope", english.format("report.patient.notAccredited"));
        assertEquals("Performed by St Mary's Reference Lab",
                english.format("report.patient.performedBy", "St Mary's Reference Lab"));
    }
}
