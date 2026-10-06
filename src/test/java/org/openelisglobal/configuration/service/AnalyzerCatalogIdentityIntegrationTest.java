package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.AppTestConfig;
import org.openelisglobal.BaseTestConfig;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerMappingDefaults;
import org.openelisglobal.analyzer.service.AnalyzerUnresolvedReason;
import org.openelisglobal.analyzer.service.BridgeAnalyzerProfile;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Exercises normal configuration loading against the catalog installed by
 * Liquibase, in a database isolated from legacy suites that replace catalog
 * rows.
 */
@ContextConfiguration(inheritLocations = false, classes = { AppTestConfig.class,
        AnalyzerCatalogIdentityIntegrationTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AnalyzerCatalogIdentityIntegrationTest extends BaseWebContextSensitiveTest {

    private static final Path MOLECULAR_TESTS = Path
            .of("projects/analyzer-harness/config-templates/tests/molecular-tests.csv");
    private static final Path MOLECULAR_RESULTS = Path
            .of("projects/analyzer-harness/config-templates/test-results/molecular-test-results.csv");

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
    @Qualifier("testConfigurationHandler")
    private DomainConfigurationHandler testHandler;

    @Autowired
    @Qualifier("testSectionConfigurationHandler")
    private DomainConfigurationHandler sectionHandler;

    @Autowired
    private AnalyzerMappingDefaults defaults;

    @Autowired
    private AnalyzerMappingCatalogService mappingCatalog;

    @Autowired
    @Qualifier("dictionaryConfigurationHandler")
    private DomainConfigurationHandler dictionaryHandler;

    @Autowired
    @Qualifier("testResultConfigurationHandler")
    private DomainConfigurationHandler resultHandler;

    @Autowired
    private TestService tests;

    @Autowired
    private TestResultService results;

    @Autowired
    private TypeOfSampleTestService specimens;

    @Test
    public void harnessCatalogPreservesExistingCovidReportAndResultDefinitions() throws Exception {
        var original = tests.getTestByDescription("COVIDPCR(Respiratory Swab)");
        assertNotNull("The real database migration must supply the original test", original);
        assertEquals("SARS-CoV-2 RNA by qRT-PCR", original.getLocalizedReportingName().getEnglish());
        var ids = catalogIds();
        var resultIds = results.getAllMatching("test.id", original.getId()).stream().map(result -> result.getId())
                .sorted().toList();
        assertTrue("The populated catalog must include result definitions", !resultIds.isEmpty());
        var specimenIds = specimenIds(original.getId());

        for (int run = 0; run < 2; run++) {
            try (InputStream csv = Files.newInputStream(MOLECULAR_TESTS)) {
                assertNotNull(csv);
                testHandler.processConfiguration(csv, "molecular-tests.csv");
            }
            assertEquals(testHandler.getLastSummary().getRows().toString(), 0,
                    testHandler.getLastSummary().getSkipped());
            assertEquals(ids, catalogIds());
            assertEquals(original.getId(), tests.getTestByDescription("COVIDPCR(Respiratory Swab)").getId());
            assertEquals("SARS-CoV-2 RNA by qRT-PCR",
                    tests.get(original.getId()).getLocalizedReportingName().getEnglish());
            assertEquals(specimenIds, specimenIds(original.getId()));
            assertEquals(resultIds, results.getAllMatching("test.id", original.getId()).stream()
                    .map(result -> result.getId()).sorted().toList());
        }
    }

    @Test
    public void sharedCodeUpdatesOnlyTheRequestedSpecimenWithoutRenamingOrWideningAnotherTest() throws Exception {
        var respiratory = tests.getTestByDescription("COVIDPCR(Respiratory Swab)");
        var sputum = tests.getTestByDescription("COVIDPCR(Sputum)");
        assertNotNull(respiratory);
        assertNotNull(sputum);
        String before = sputum.getIsReportable();
        String originalCode = sputum.getLocalCode();
        String requested = "Y".equals(before) ? "N" : "Y";
        var ids = catalogIds();
        var respiratorySpecimens = specimenIds(respiratory.getId());
        var sputumSpecimens = specimenIds(sputum.getId());
        String csv = "testName,testSection,sampleType,localCode,isReportable,localization:en\n"
                + "COVIDPCR(Sputum),Molecular Biology,Sputum,covidpcr," + requested + ",COVID-19 PCR\n";
        try {
            testHandler.processConfiguration(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)),
                    "specimen-specific-update.csv");
            assertEquals(testHandler.getLastSummary().getRows().toString(), 0,
                    testHandler.getLastSummary().getSkipped());
            assertEquals(1, testHandler.getLastSummary().getUpdated());
            assertEquals(ids, catalogIds());
            assertEquals(requested, tests.get(sputum.getId()).getIsReportable());
            assertEquals(respiratory.getIsReportable(), tests.get(respiratory.getId()).getIsReportable());
            assertEquals(respiratorySpecimens, specimenIds(respiratory.getId()));
            assertEquals(sputumSpecimens, specimenIds(sputum.getId()));
        } finally {
            var restored = tests.get(sputum.getId());
            restored.setIsReportable(before);
            restored.setLocalCode(originalCode);
            restored.setSysUserId(TEST_SYS_USER_ID);
            tests.update(restored);
        }
    }

    @Test
    public void harnessMolecularCatalogResolvesSpecimenAndReportedResistanceOutcomes() throws Exception {
        var plasma = tests.getTestByDescription("HIVVIRALLOAD(Plasma)");
        var serum = tests.getTestByDescription("HIVVIRALLOAD(Serum)");
        assertNotNull(plasma);
        assertNotNull(serum);
        String plasmaId = plasma.getId();
        String serumId = serum.getId();
        var plasmaSpecimens = specimenIds(plasmaId);
        var serumSpecimens = specimenIds(serumId);
        try (InputStream csv = getClass().getResourceAsStream("/configuration/test-sections/molecular-sections.csv")) {
            sectionHandler.processConfiguration(csv, "molecular-sections.csv");
        }
        try (InputStream csv = Files.newInputStream(MOLECULAR_TESTS)) {
            testHandler.processConfiguration(csv, "molecular-tests.csv");
        }
        assertEquals(testHandler.getLastSummary().getRows().toString(), 0, testHandler.getLastSummary().getSkipped());
        var profile = new ObjectMapper().readTree(
                """
                        {"profileMeta":{"id":"fixture.specimen","displayName":"Specimen default"},
                        "protocol":{"name":"ASTM"},
                        "catalog":{"revision":1,"revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","source":"SITE","status":"ACTIVE"},
                        "default_test_mappings":[{"test_code":"RAW-VL","loinc":"20447-9","result_type":"quantitative"}]}
                        """);
        var viralLoad = defaults.resolve(BridgeAnalyzerProfile.from(profile)).tests().get(0);
        assertNull(viralLoad.testId());
        assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, viralLoad.unresolvedReason());
        assertEquals(plasmaSpecimens, specimenIds(plasmaId));
        assertEquals(serumSpecimens, specimenIds(serumId));
        assertEquals("20447-9", tests.get(serumId).getLoinc());
        assertTrue(results.getActiveTestResultsByTest(plasmaId).stream()
                .anyMatch(result -> "N".equals(result.getTestResultType())));
        try (InputStream csv = getClass()
                .getResourceAsStream("/configuration/dictionaries/analyzer-result-options.csv")) {
            dictionaryHandler.processConfiguration(csv, "analyzer-result-options.csv");
        }
        var rif = tests.getTestByDescription("Xpert RIF Resistance");
        assertNotNull(rif);
        var shipped = BridgeAnalyzerProfile.from(new ObjectMapper().readTree(
                Path.of("tools/openelis-analyzer-bridge/src/main/resources/analyzer-profiles/genexpert-astm-v7.json")
                        .toFile()));
        Map<String, String> firstOptions = null;
        for (int run = 0; run < 2; run++) {
            try (InputStream csv = Files.newInputStream(MOLECULAR_RESULTS)) {
                resultHandler.processConfiguration(csv, "molecular-test-results.csv");
            }
            var options = mappingCatalog.getActiveResultOptions(rif.getId()).stream()
                    .collect(Collectors.toMap(option -> option.label(), option -> option.id()));
            assertEquals(java.util.Set.of("DETECTED", "NOT DETECTED", "Indeterminate"), options.keySet());
            if (firstOptions != null)
                assertEquals(firstOptions, options);
            firstOptions = options;
            var rifAnswers = defaults.resolve(shipped).results().stream()
                    .filter(row -> "RIF".equals(row.sourceRowKey())).toList();
            assertEquals(java.util.Set.of("DETECTED", "NOT DETECTED", "INDETERMINATE"),
                    rifAnswers.stream().map(row -> row.rawValue()).collect(Collectors.toSet()));
            rifAnswers.forEach(row -> {
                assertNull(row.testResultId());
                assertNotNull(row.unresolvedReason());
            });
        }
        var resolved = defaults.resolve(shipped);
        var covidRow = resolved.tests().stream().filter(row -> "COVID19".equals(row.sourceRowKey())).findFirst()
                .orElseThrow();
        assertNull(covidRow.testId());
        assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, covidRow.unresolvedReason());
        resolved.results().stream().filter(row -> "COVID19".equals(row.sourceRowKey())).forEach(row -> {
            assertNull(row.testResultId());
            assertEquals(AnalyzerUnresolvedReason.AMBIGUOUS, row.unresolvedReason());
        });

    }

    private Map<String, String> catalogIds() {
        return tests.getTestsByLoincCode("94500-6").stream()
                .collect(Collectors.toMap(test -> test.getDescription(), test -> test.getId()));
    }

    private List<String> specimenIds(String testId) {
        return specimens.getTypeOfSampleTestsForTest(testId).stream().map(link -> link.getTypeOfSampleId()).sorted()
                .toList();
    }
}
