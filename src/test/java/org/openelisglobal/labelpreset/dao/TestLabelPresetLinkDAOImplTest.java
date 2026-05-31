package org.openelisglobal.labelpreset.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO tests for {@link TestLabelPresetLinkDAO} (OGC-285 M2, task T028).
 *
 * <p>
 * Real DAO + real DB, mirroring {@code StorageShelfDAOTest}: each persisted
 * parent FK row is created, then the DAO under test is exercised, then test
 * data is cleaned in {@link Before}/{@link After} (BaseWebContextSensitiveTest
 * is {@code Propagation.NOT_SUPPORTED} — DAO writes commit and are NOT rolled
 * back, so leftover rows from a prior run must be removed; {@code label_preset}
 * has a {@code UNIQUE(name)} constraint).
 *
 * <p>
 * The required {@code @ManyToOne} to the legacy {@code Test} is satisfied by
 * loading {@code testdata/test.xml} (persisted Test id=1 "Blood Test" and id=2
 * "Urine Test", the same fixture {@code TestServiceTest} uses) and obtaining
 * the managed entity via {@link TestService}. The {@code LabelPreset} parents
 * are built CHECK-valid and inserted through {@link LabelPresetDAO}.
 *
 * <p>
 * Reference test {@code AnalyzerPendingCodeDAOImplTest} is a pure-Mockito unit
 * test; honoring it literally would violate the non-negotiable real-DB /
 * no-mock discipline and tasks.md's "with real DB" for T028, so the DB-backed
 * {@code StorageShelfDAOTest} is mirrored instead.
 */
public class TestLabelPresetLinkDAOImplTest extends BaseWebContextSensitiveTest {

    private static final String LINK_PRESET_A_NAME = "T028-Link-Preset-A";
    private static final String LINK_PRESET_B_NAME = "T028-Link-Preset-B";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TestLabelPresetLinkDAO testLabelPresetLinkDAO;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestService testService;

    private JdbcTemplate jdbcTemplate;

    // Persisted legacy Test rows from testdata/test.xml.
    private org.openelisglobal.test.valueholder.Test bloodTest; // id "1"
    private org.openelisglobal.test.valueholder.Test urineTest; // id "2"

    private LabelPreset presetA;
    private LabelPreset presetB;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        // Load persisted Test rows (id=1, id=2) the same way TestServiceTest does.
        executeDataSetWithStateManagement("testdata/test.xml");
        cleanTestData();

        bloodTest = testService.getTestById("1");
        urineTest = testService.getTestById("2");
        assertNotNull("fixture Test id=1 should be persisted", bloodTest);
        assertNotNull("fixture Test id=2 should be persisted", urineTest);

        presetA = saveLabelPreset(LINK_PRESET_A_NAME);
        presetB = saveLabelPreset(LINK_PRESET_B_NAME);
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    /**
     * Remove this test's rows in FK order (link -> preset). Matches by the
     * test-specific preset names so a crashed prior run cannot collide with
     * {@code label_preset}'s UNIQUE(name).
     */
    private void cleanTestData() {
        try {
            jdbcTemplate.update(
                    "DELETE FROM test_label_preset_link WHERE preset_id IN"
                            + " (SELECT id FROM label_preset WHERE name IN (?, ?))",
                    LINK_PRESET_A_NAME, LINK_PRESET_B_NAME);
            jdbcTemplate.update("DELETE FROM label_preset WHERE name IN (?, ?)", LINK_PRESET_A_NAME,
                    LINK_PRESET_B_NAME);
        } catch (Exception e) {
            // Ignore cleanup errors - next test run will retry.
        }
    }

    private LabelPreset saveLabelPreset(String name) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        preset.setHeightMm(25); // 5..200
        preset.setWidthMm(50); // 5..200
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerOrder(false);
        preset.setPrintsPerSample(true);
        preset.setDefaultPerOrder(0);
        preset.setMaxPerOrder(10);
        preset.setDefaultPerSample(1);
        preset.setMaxPerSample(5);
        preset.setIsSystem(false);
        preset.setIsActive(true);
        Integer id = labelPresetDAO.insert(preset);
        assertNotNull("LabelPreset insert should return a generated id", id);
        return preset;
    }

    private TestLabelPresetLink newLink(org.openelisglobal.test.valueholder.Test test, LabelPreset preset,
            int defaultQty, int maxQty) {
        TestLabelPresetLink link = new TestLabelPresetLink();
        link.setTest(test);
        link.setPreset(preset);
        link.setDefaultQty(defaultQty);
        link.setMaxQty(maxQty);
        link.setAllowOverride(false);
        return link;
    }

    /**
     * insert + getById: round-trips every persisted column and both FK ids. With
     * {@code Propagation.NOT_SUPPORTED}, {@code get(id)} issues a fresh SELECT, so
     * this catches a broken {@code @Column}/{@code @JoinColumn} mapping.
     */
    @Test
    public void insertAndGetById_roundTripsAllColumns() {
        TestLabelPresetLink link = newLink(bloodTest, presetA, 2, 4);

        Integer id = testLabelPresetLinkDAO.insert(link);
        assertNotNull("insert should return a generated id", id);

        TestLabelPresetLink fetched = testLabelPresetLinkDAO.get(id).orElse(null);
        assertNotNull("getById should return the inserted link", fetched);
        assertEquals("test FK should round-trip", "1", fetched.getTest().getId());
        assertEquals("preset FK should round-trip", presetA.getId(), fetched.getPreset().getId());
        assertEquals("defaultQty should round-trip", Integer.valueOf(2), fetched.getDefaultQty());
        assertEquals("maxQty should round-trip", Integer.valueOf(4), fetched.getMaxQty());
        assertEquals("allowOverride should round-trip", Boolean.FALSE, fetched.getAllowOverride());

        // Inverse of the ORM read path: confirm the row physically persisted with
        // the expected FK columns (independent of Hibernate mapping).
        Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM test_label_preset_link WHERE id = ? AND test_id = 1 AND preset_id = ?",
                Long.class, id, presetA.getId());
        assertEquals("exactly one row with the expected FKs should exist", Long.valueOf(1L), rowCount);
    }

    /**
     * listByTestId returns only the links for the requested test. A second link on
     * a different test (id=2) is the decoy that fails if the {@code WHERE
     * l.test.id = :testId} clause is dropped.
     */
    @Test
    public void listByTestId_returnsOnlyMatchingTestExcludesOthers() {
        TestLabelPresetLink bloodLink = newLink(bloodTest, presetA, 1, 3);
        TestLabelPresetLink urineLink = newLink(urineTest, presetB, 1, 3);
        Integer bloodLinkId = testLabelPresetLinkDAO.insert(bloodLink);
        testLabelPresetLinkDAO.insert(urineLink);

        List<TestLabelPresetLink> results = testLabelPresetLinkDAO.listByTestId("1");

        // The exclusion assertion below is what carries inversion-worthiness; the
        // size==1 check is tighter and holds because this class is currently the
        // only writer of test_label_preset_link and @Before/@After delete the A/B
        // links. A future M3 service test that commits a link on shared Test id=1
        // would need to revisit this size coupling.
        assertEquals("only the test-1 link should be returned", 1, results.size());
        assertEquals(bloodLinkId, results.get(0).getId());
        assertEquals("1", results.get(0).getTest().getId());
        assertTrue("result should be test-1's link", results.stream().anyMatch(l -> "1".equals(l.getTest().getId())));
        assertFalse("test-2's link must be excluded", results.stream().anyMatch(l -> "2".equals(l.getTest().getId())));
    }

    /**
     * listByPresetId returns only the links for the requested preset. A second link
     * on a different preset (presetB) is the decoy that fails if the {@code WHERE
     * l.preset.id = :presetId} clause is dropped.
     */
    @Test
    public void listByPresetId_returnsOnlyMatchingPresetExcludesOthers() {
        TestLabelPresetLink linkOnA = newLink(bloodTest, presetA, 1, 3);
        TestLabelPresetLink linkOnB = newLink(urineTest, presetB, 1, 3);
        Integer linkOnAId = testLabelPresetLinkDAO.insert(linkOnA);
        testLabelPresetLinkDAO.insert(linkOnB);

        List<TestLabelPresetLink> results = testLabelPresetLinkDAO.listByPresetId(presetA.getId());

        assertEquals("only the presetA link should be returned", 1, results.size());
        assertEquals(linkOnAId, results.get(0).getId());
        assertEquals(presetA.getId(), results.get(0).getPreset().getId());
        assertFalse("presetB's link must be excluded",
                results.stream().anyMatch(l -> presetB.getId().equals(l.getPreset().getId())));
    }
}
