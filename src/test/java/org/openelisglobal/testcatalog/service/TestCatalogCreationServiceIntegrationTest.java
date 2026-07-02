package org.openelisglobal.testcatalog.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.localization.service.LocalizationService;
import org.openelisglobal.localization.service.LocalizationServiceImpl;
import org.openelisglobal.localization.valueholder.Localization;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-1112 (FR-2..4) — create-in-place backing service. Verifies a minimal test
 * is created Inactive with its sample-type link, and that code uniqueness is
 * detected.
 */
public class TestCatalogCreationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long SAMPLE_TYPE_ID = 953080L;
    private static final String SAMPLE_TYPE_DESC = "CreateIT-Serum";
    private static final String CODE = "CreateIT-CODE";

    @Autowired
    private TestCatalogCreationService creationService;
    @Autowired
    private TestService testService;
    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;
    @Autowired
    private LocalizationService localizationService;
    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String createdTestId;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        Localization sampleTypeName = LocalizationServiceImpl.createNewLocalization(SAMPLE_TYPE_DESC, SAMPLE_TYPE_DESC,
                LocalizationServiceImpl.LocalizationType.SAMPLE_TYPE_NAME);
        sampleTypeName.setSysUserId("1");
        String locId = localizationService.insert(sampleTypeName);
        jdbc.update(
                "INSERT INTO clinlims.type_of_sample (id, description, name_localization_id, is_active, lastupdated)"
                        + " VALUES (?, ?, ?, true, NOW())",
                SAMPLE_TYPE_ID, SAMPLE_TYPE_DESC, Long.parseLong(locId));
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        if (createdTestId != null) {
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE test_id = ?", Long.parseLong(createdTestId));
            jdbc.update("DELETE FROM clinlims.test WHERE id = ?", Long.parseLong(createdTestId));
            createdTestId = null;
        }
        jdbc.update("DELETE FROM clinlims.test WHERE local_code = ?", CODE);
        java.util.List<Long> locIds = jdbc.queryForList(
                "SELECT name_localization_id FROM clinlims.type_of_sample WHERE id = ?", Long.class, SAMPLE_TYPE_ID);
        jdbc.update("DELETE FROM clinlims.type_of_sample WHERE id = ?", SAMPLE_TYPE_ID);
        for (Long id : locIds) {
            if (id != null) {
                try {
                    localizationService.delete(String.valueOf(id), "1");
                } catch (RuntimeException ignored) {
                    // already gone
                }
            }
        }
    }

    @Test
    public void createInactiveTest_createsInactiveTestWithSampleTypeLink() {
        TestCatalogCreationService.CreateTestParams params = new TestCatalogCreationService.CreateTestParams();
        params.name = "CreateIT " + UUID.randomUUID();
        params.reportingName = params.name;
        params.code = CODE;
        params.sampleTypeId = String.valueOf(SAMPLE_TYPE_ID);
        params.domain = "CLINICAL";
        params.orderable = true;

        createdTestId = creationService.createInactiveTest(params, "1");
        assertNotNull(createdTestId);

        org.openelisglobal.test.valueholder.Test created = testService.getTestById(createdTestId);
        assertNotNull(created);
        assertFalse("new test must start Inactive", created.isActive());
        assertEquals(CODE, created.getLocalCode());
        assertFalse("sample-type link must be created",
                typeOfSampleTestService.getTypeOfSampleTestsForTest(createdTestId).isEmpty());
        assertTrue("code must now be reported in use", creationService.codeInUse(CODE));
    }

    @Test
    public void codeInUse_falseForUnusedCode() {
        assertFalse(creationService.codeInUse("no-such-code-" + UUID.randomUUID()));
    }
}
