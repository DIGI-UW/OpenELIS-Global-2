package org.openelisglobal.configuration.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.openelisglobal.AppTestConfig;
import org.openelisglobal.BaseTestConfig;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
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
    private TestService tests;

    @Autowired
    private TypeOfSampleTestService specimens;

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

    private Map<String, String> catalogIds() {
        return tests.getTestsByLoincCode("94500-6").stream()
                .collect(Collectors.toMap(test -> test.getDescription(), test -> test.getId()));
    }

    private List<String> specimenIds(String testId) {
        return specimens.getTypeOfSampleTestsForTest(testId).stream().map(link -> link.getTypeOfSampleId()).sorted()
                .toList();
    }
}
