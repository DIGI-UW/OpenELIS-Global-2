package org.openelisglobal.eqa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.eqa.service.EQAPanelService;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAProgram;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Which tests a panel can be built from, and where the analyte behind a panel
 * sample comes from.
 *
 * <p>
 * A test reaches a panel wizard if a participating laboratory could raise an
 * order for it, which is the half of the loop no deployment can repair from
 * inside the application. The analyte a target is stored against is the half it
 * can: no administration screen writes one, so it is created on write from the
 * test's own name.
 */
public class EQAPanelMaterialEligibilityIntegrationTest extends EQASpineTestBase {

    private static final long ORDERABLE_TEST = 9941L;
    private static final long UNORDERABLE_TEST = 9942L;
    private static final int SAMPLE_TYPE_LINK = 99441;

    @Autowired
    private EQAPanelService eqaPanelService;

    private EQAProgram scheme;

    @Before
    public void seedCatalog() {
        seedTest(ORDERABLE_TEST, "Eligibility orderable test");
        seedTest(UNORDERABLE_TEST, "Eligibility unorderable test");
        // An existing sample type, not a new one: type_of_sample requires a
        // localization row, and which type it is does not matter here.
        Long sampleType = jdbc.queryForObject("SELECT min(id) FROM clinlims.type_of_sample", Long.class);
        jdbc.update("DELETE FROM clinlims.sampletype_test WHERE id = ?", SAMPLE_TYPE_LINK);
        jdbc.update("INSERT INTO clinlims.sampletype_test (id, sample_type_id, test_id) VALUES (?, ?, ?)",
                SAMPLE_TYPE_LINK, sampleType, ORDERABLE_TEST);

        scheme = insertScheme("Eligibility scheme " + System.nanoTime(), EQASchemeType.REGIONAL_PT, "CPHL");
    }

    @Override
    protected void cleanEqaTables() {
        super.cleanEqaTables();
        if (jdbc != null) {
            jdbc.update("DELETE FROM clinlims.test_analyte WHERE test_id IN (?, ?)", ORDERABLE_TEST, UNORDERABLE_TEST);
            jdbc.update("DELETE FROM clinlims.analyte WHERE name LIKE 'Eligibility %'");
            jdbc.update("DELETE FROM clinlims.sampletype_test WHERE id = ?", SAMPLE_TYPE_LINK);
            jdbc.update("DELETE FROM clinlims.test WHERE id IN (?, ?)", ORDERABLE_TEST, UNORDERABLE_TEST);
        }
    }

    @Test
    public void aTestAParticipantCanOrderIsOfferedEvenWithNoAnalyte() {
        assertEquals("the fixture test starts with no analyte", Integer.valueOf(0), analyteLinkCount(ORDERABLE_TEST));

        List<String> offered = eqaPanelService.getTestableTestIds();

        assertTrue("a test with a sample type is panel material", offered.contains(String.valueOf(ORDERABLE_TEST)));
        assertFalse("a test no laboratory can order is a dead end", offered.contains(String.valueOf(UNORDERABLE_TEST)));
    }

    @Test
    public void theAnalyteIsCreatedFromTheTestNameOnFirstUseAndReusedAfterwards() {
        Long analyteId = eqaPanelService.analyteIdForTest(String.valueOf(ORDERABLE_TEST));

        assertNotNull(analyteId);
        assertEquals("Eligibility orderable test",
                jdbc.queryForObject("SELECT name FROM clinlims.analyte WHERE id = ?", String.class, analyteId));
        assertEquals(Integer.valueOf(1), analyteLinkCount(ORDERABLE_TEST));

        assertEquals("a second call reuses the analyte rather than minting another", analyteId,
                eqaPanelService.analyteIdForTest(String.valueOf(ORDERABLE_TEST)));
        assertEquals(Integer.valueOf(1), analyteLinkCount(ORDERABLE_TEST));
    }

    @Test
    public void aPanelSampleCanBeWrittenForATestTheCatalogNeverGaveAnAnalyte() {
        EQAPanelSample sample = new EQAPanelSample();
        sample.setSampleCode("E01");
        sample.setAnalyteId(eqaPanelService.analyteIdForTest(String.valueOf(ORDERABLE_TEST)));
        sample.setTargetValue("40");
        sample.setSysUserId(USER);

        EQAPanel panel = new EQAPanel();
        panel.setScheme(scheme);
        panel.setPanelName("Eligibility panel");
        panel.setSysUserId(USER);
        EQAPanel created = eqaPanelService.create(panel, List.of(sample), USER);

        assertEquals(Integer.valueOf(1),
                jdbc.queryForObject(
                        "SELECT count(*) FROM clinlims.eqa_panel_sample WHERE panel_id = ? AND analyte_id IS NOT NULL",
                        Integer.class, created.getId()));
    }

    // ---- helpers ----

    private Integer analyteLinkCount(long testId) {
        return jdbc.queryForObject("SELECT count(*) FROM clinlims.test_analyte WHERE test_id = ?", Integer.class,
                testId);
    }

    private void seedTest(long id, String name) {
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " SELECT ?, ?, ?, 'Y', ?, now() WHERE NOT EXISTS (SELECT 1 FROM clinlims.test WHERE id = ?)",
                id, name, name, UUID.randomUUID().toString(), id);
    }
}
