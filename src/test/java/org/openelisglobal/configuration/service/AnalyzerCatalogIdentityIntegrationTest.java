package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
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

    private Map<String, String> catalogIds() {
        return tests.getTestsByLoincCode("94500-6").stream()
                .collect(Collectors.toMap(test -> test.getDescription(), test -> test.getId()));
    }

    private List<String> specimenIds(String testId) {
        return specimens.getTypeOfSampleTestsForTest(testId).stream().map(link -> link.getTypeOfSampleId()).sorted()
                .toList();
    }
}
