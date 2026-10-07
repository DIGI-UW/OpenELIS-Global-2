package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.Test;
import org.openelisglobal.AppTestConfig;
import org.openelisglobal.BaseTestConfig;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerMappingDefaults;
import org.openelisglobal.analyzer.service.AnalyzerMappingDraft;
import org.openelisglobal.analyzer.service.BridgeAnalyzerProfile;
import org.openelisglobal.analyzer.valueholder.AnalyzerMappingState;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * The analyzer harness runs on its own copy of the generic dictionary. Loaded
 * the way OpenELIS loads configuration at startup, it lets every baseline
 * profile the Bridge ships bind every test, record and value with no operator
 * work.
 */
@ContextConfiguration(inheritLocations = false, classes = { AppTestConfig.class,
        HarnessDictionaryIntegrationTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class HarnessDictionaryIntegrationTest extends BaseWebContextSensitiveTest {

    private static final Path DICTIONARY = Path.of("projects", "analyzer-harness", "dictionary");
    private static final Path PROFILES = Path.of("tools", "openelis-analyzer-bridge", "src", "main", "resources",
            "analyzer-profiles");
    private static final List<String> BASELINE_PROFILES = List.of("cepheid-genexpert-astm.json",
            "hain-fluorocycler-xt.json", "thermo-quantstudio.json");
    private static final Set<String> CATALOG_DOMAINS = Set.of("sample-types", "test-sections", "tests", "dictionaries",
            "result-components", "test-results", "answer-terminology");

    @Configuration
    public static class TestConfig extends BaseTestConfig {
        private final PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:14.4");

        @Override
        protected PostgreSQLContainer<?> databaseContainer() {
            return database;
        }

        @PreDestroy
        public void stopDatabase() {
            database.stop();
        }
    }

    @Autowired
    private ConfigurationInitializationService configuration;

    @Autowired
    private AnalyzerMappingDefaults defaults;

    @Autowired
    private TestService tests;

    @Autowired
    private DataSource dataSource;

    @Test
    public void everyBaselineProfileBindsEveryTestRecordAndValue() throws Exception {
        loadHarnessDictionary();

        List<String> unbound = new ArrayList<>();
        for (String profileFile : BASELINE_PROFILES) {
            BridgeAnalyzerProfile profile = BridgeAnalyzerProfile
                    .from(new ObjectMapper().readTree(PROFILES.resolve(profileFile).toFile()));
            AnalyzerMappingDraft draft = defaults.resolve(profile);
            draft.tests().stream().filter(row -> row.mappingState() != AnalyzerMappingState.BOUND)
                    .forEach(row -> unbound.add(profileFile + " " + row.sourceRowKey() + " " + row.subIdentity() + ": "
                            + row.unresolvedReason()));
            draft.results().stream().filter(row -> row.mappingState() != AnalyzerMappingState.BOUND)
                    .forEach(row -> unbound.add(profileFile + " " + row.sourceRowKey() + " " + row.subIdentity() + " = "
                            + row.rawValue() + ": " + row.unresolvedReason()));
        }

        assertEquals("rows a fresh setup would leave unresolved", List.of(), unbound);
    }

    /**
     * The analyzer codes are unambiguous: no two active tests carry one on the same
     * specimen. Other tests in the generic dictionary may share a code.
     */
    @Test
    public void noTwoActiveTestsShareAnAnalyzerLoincOnASpecimenAndTheCovidReportFindsItsTest() throws Exception {
        loadHarnessDictionary();
        List<String> analyzerLoincs = new ArrayList<>();
        for (String profileFile : BASELINE_PROFILES) {
            new ObjectMapper().readTree(PROFILES.resolve(profileFile).toFile()).path("default_test_mappings")
                    .forEach(test -> analyzerLoincs.add(test.path("loinc").asText()));
        }

        List<String> shared = new JdbcTemplate(dataSource).queryForList(
                "SELECT t.loinc || ' on ' || s.description FROM clinlims.test t"
                        + " JOIN clinlims.sampletype_test st ON st.test_id = t.id"
                        + " JOIN clinlims.type_of_sample s ON s.id = st.sample_type_id"
                        + " WHERE t.is_active = 'Y' AND t.loinc = ANY (?)"
                        + " GROUP BY t.loinc, s.description HAVING count(*) > 1",
                String.class, (Object) analyzerLoincs.toArray(String[]::new));
        assertEquals(List.of(), shared);
        assertFalse(tests.getActiveTestsByLoinc(new String[] { "94547-7", "94500-6" }).isEmpty());
    }

    private void loadHarnessDictionary() throws IOException {
        Path copy = Files.createTempDirectory("harness-dictionary");
        try (Stream<Path> files = Files.walk(DICTIONARY)) {
            for (Path source : files.toList()) {
                Path target = copy.resolve(DICTIONARY.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
        ReflectionTestUtils.setField(configuration, "configurationBaseDir", copy.toString());
        configuration.reload(new ConfigurationReloadOptions(CATALOG_DOMAINS, true));
    }
}
