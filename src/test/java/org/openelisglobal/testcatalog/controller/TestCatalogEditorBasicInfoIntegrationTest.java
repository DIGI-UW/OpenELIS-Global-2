package org.openelisglobal.testcatalog.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController;
import org.openelisglobal.testcatalog.controller.rest.TestCatalogEditorRestController.BasicInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-748 Basic Info — round-trip against a real DB: load a test's basic-info,
 * change Domain + AMR + status, save, reload, and confirm the M1 columns
 * (TEST.DOMAIN, TEST.ANTIMICROBIAL_RESISTANCE) and status persisted.
 */
public class TestCatalogEditorBasicInfoIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95001L;

    @Autowired
    private TestService testService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private TestCatalogEditorRestController controller;
    private JdbcTemplate jdbc;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        // Controllers live in the servlet context, not the test's root context; build
        // one with the real (autowired) service so the save logic hits a real DB.
        controller = new TestCatalogEditorRestController();
        org.springframework.test.util.ReflectionTestUtils.setField(controller, "testService", testService);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain, antimicrobial_resistance,"
                        + " orderable, lastupdated) VALUES (?, ?, ?, 'Y', ?, 'CLINICAL', false, true, NOW())",
                TEST_ID, "BasicInfoIT", "BasicInfoIT desc", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    @org.junit.Test
    public void basicInfo_roundTrips_domainAmrAndStatus() {
        ResponseEntity<BasicInfo> loaded = controller.getBasicInfo(String.valueOf(TEST_ID));
        assertEquals(200, loaded.getStatusCode().value());
        assertEquals("CLINICAL", loaded.getBody().domain);
        assertTrue(!loaded.getBody().antimicrobialResistance);

        BasicInfo update = new BasicInfo();
        update.domain = "VECTOR";
        update.antimicrobialResistance = true;
        update.active = true;
        update.orderable = false;
        ResponseEntity<BasicInfo> saved = controller.saveBasicInfo(String.valueOf(TEST_ID), update);
        assertEquals(200, saved.getStatusCode().value());
        assertEquals("VECTOR", saved.getBody().domain);
        assertTrue(saved.getBody().antimicrobialResistance);

        // Reload from DB through the service to confirm persistence.
        Test reloaded = testService.getTestById(String.valueOf(TEST_ID));
        assertEquals("VECTOR", reloaded.getDomain());
        assertTrue(Boolean.TRUE.equals(reloaded.getAntimicrobialResistance()));
        assertTrue(!Boolean.TRUE.equals(reloaded.getOrderable()));
    }

    @org.junit.Test
    public void basicInfo_rejectsInvalidDomain() {
        BasicInfo bad = new BasicInfo();
        bad.domain = "NONSENSE";
        ResponseEntity<BasicInfo> resp = controller.saveBasicInfo(String.valueOf(TEST_ID), bad);
        assertEquals(422, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void basicInfo_unknownTestReturns404() {
        ResponseEntity<BasicInfo> resp = controller.getBasicInfo("99999999");
        assertEquals(404, resp.getStatusCode().value());
    }

    @org.junit.Test
    public void listTests_filtersByDomainAndPaginates() {
        // Seed three catalog rows: 2 CLINICAL, 1 VECTOR (ids in a cleaned range).
        for (long id = 95010L; id <= 95012L; id++) {
            String dom = id == 95012L ? "VECTOR" : "CLINICAL";
            jdbc.update(
                    "INSERT INTO clinlims.test (id, name, description, is_active, guid, domain,"
                            + " antimicrobial_resistance, orderable, lastupdated)"
                            + " VALUES (?, ?, ?, 'Y', ?, ?, false, true, NOW())",
                    id, "ListIT-" + id, "ListIT-" + id, UUID.randomUUID().toString(), dom);
        }

        TestCatalogEditorRestController.TestListPage vector = controller.listTests("VECTOR", "all", null, "ListIT-", 1,
                25);
        assertEquals(1, vector.total);
        assertEquals("VECTOR", vector.rows.get(0).domain);

        TestCatalogEditorRestController.TestListPage all = controller.listTests(null, "all", null, "ListIT-", 1, 2);
        assertEquals(3, all.total);
        assertEquals(2, all.rows.size()); // page size 2 of 3 total

        TestCatalogEditorRestController.TestListPage page2 = controller.listTests(null, "all", null, "ListIT-", 2, 2);
        assertEquals(1, page2.rows.size());
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE FROM clinlims.test WHERE id = ? OR (id >= 95010 AND id <= 95012)", TEST_ID);
        } catch (Exception ignored) {
            // ignore
        }
    }
}
