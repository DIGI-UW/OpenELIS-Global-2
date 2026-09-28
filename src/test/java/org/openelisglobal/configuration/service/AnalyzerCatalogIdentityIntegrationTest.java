package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerMappingCatalogService;
import org.openelisglobal.analyzer.service.AnalyzerMappingDefaults;
import org.openelisglobal.analyzer.service.BridgeAnalyzerProfile;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Exercises normal configuration loading against the catalog installed by
 * Liquibase.
 */
public class AnalyzerCatalogIdentityIntegrationTest extends BaseWebContextSensitiveTest {

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
    public void bundledCovidConfigurationReusesExistingIdsAndResultDefinitionsOnRepeatLoad() throws Exception {
        var original = tests.getTestByDescription("COVIDPCR(Respiratory Swab)");
        assertNotNull("The real database migration must supply the original test", original);
        var ids = catalogIds();
        var resultIds = results.getAllMatching("test.id", original.getId()).stream().map(result -> result.getId())
                .sorted().toList();
        assertTrue("The populated catalog must include result definitions", !resultIds.isEmpty());
        var specimenIds = specimenIds(original.getId());

        for (int run = 0; run < 2; run++) {
            try (InputStream csv = getClass().getResourceAsStream("/configuration/tests/analyzer-covid-tests.csv")) {
                assertNotNull(csv);
                testHandler.processConfiguration(csv, "analyzer-covid-tests.csv");
            }
            assertEquals(0, testHandler.getLastSummary().getCreated());
            assertEquals(1, testHandler.getLastSummary().getUpdated());
            assertEquals(0, testHandler.getLastSummary().getSkipped());
            assertEquals(ids, catalogIds());
            assertEquals(original.getId(), tests.getTestByDescription("COVIDPCR(Respiratory Swab)").getId());
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
            assertEquals(0, testHandler.getLastSummary().getSkipped());
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
    public void bundledMolecularDefaultsResolveSpecimenAndReportedResistanceOutcomes() throws Exception {
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
        try (InputStream csv = getClass().getResourceAsStream("/configuration/tests/molecular-tests.csv")) {
            testHandler.processConfiguration(csv, "molecular-tests.csv");
        }
        assertEquals(0, testHandler.getLastSummary().getSkipped());
        var profile = new ObjectMapper().readTree(
                """
                        {"profileMeta":{"id":"fixture.specimen","displayName":"Specimen default"},
                        "protocol":{"name":"ASTM"},
                        "catalog":{"revision":1,"revisionFingerprint":"sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa","source":"SITE","status":"ACTIVE"},
                        "default_test_mappings":[{"test_code":"RAW-VL","loinc":"20447-9","result_type":"quantitative","specimen_type_hint":"Plasma"}]}
                        """);
        assertEquals(plasmaId, defaults.resolve(BridgeAnalyzerProfile.from(profile)).tests().get(0).testId());
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
                Path.of("tools/openelis-analyzer-bridge/src/main/resources/analyzer-profiles/genexpert-astm-v5.json")
                        .toFile()));
        Map<String, String> firstOptions = null;
        for (int run = 0; run < 2; run++) {
            try (InputStream csv = getClass()
                    .getResourceAsStream("/configuration/test-results/molecular-test-results.csv")) {
                resultHandler.processConfiguration(csv, "molecular-test-results.csv");
            }
            var options = mappingCatalog.getActiveResultOptions(rif.getId()).stream()
                    .collect(Collectors.toMap(option -> option.label(), option -> option.id()));
            assertEquals(java.util.Set.of("DETECTED", "NOT DETECTED", "Indeterminate"), options.keySet());
            if (firstOptions != null)
                assertEquals(firstOptions, options);
            firstOptions = options;
            var draft = defaults.resolve(shipped);
            var bindings = draft.results().stream().filter(row -> "RIF".equals(row.sourceRowKey()))
                    .collect(Collectors.toMap(row -> row.rawValue(), row -> row.testResultId()));
            assertEquals(Map.of("DETECTED", options.get("DETECTED"), "NOT DETECTED", options.get("NOT DETECTED"),
                    "INDETERMINATE", options.get("Indeterminate")), bindings);
        }

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
