package org.openelisglobal.sample.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.SampleAddService;
import org.openelisglobal.common.services.SampleAddService.SampleTestCollection;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.OrderLabelRequestDAO;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.labelpreset.service.TestLabelPresetLinkService;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.sample.action.util.SamplePatientUpdateData;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * HARD-RULE proof for the OGC-285 M5b live wiring (FRS §7.3).
 *
 * <p>
 * Exercises the <em>real</em> save-orchestration seam the SamplePatientEntry
 * save hook calls — {@link SamplePatientEntryService#persistLabelRequests} —
 * against a real PostgreSQL DB, and asserts:
 *
 * <ol>
 * <li>{@code order_label_request} rows are written with a non-null
 * {@code preset_snapshot} (the live persistence actually fires; not a
 * commented-out / TODO hook).
 * <li><b>Positional correlation</b> holds against real persisted sample items:
 * {@code sample_id_local "0"} anchors to the first
 * {@code SampleTestCollection}'s sample item, {@code "1"} to the second — the
 * keying contract the frontend and backend must agree on.
 * <li>The per-sample snapshot captures the driving test link for the test that
 * actually belongs to that positional sample (proving the per-row
 * {@code testIdsBySampleLocal} map is built per position, not globally).
 * </ol>
 *
 * <p>
 * <b>Why this seam and not a MockMvc POST:</b> {@code AppTestConfig} excludes
 * {@code org.openelisglobal.sample.controller.*} from the component scan (see
 * {@code SamplePatientEntryRestControllerErrorPathTest}), so an end-to-end POST
 * through the REST controller isn't possible without a bespoke test context.
 * The controller hook is a null-guarded one-line delegation to this method; the
 * load-bearing positional-correlation logic lives here, in the scanned service,
 * and is driven by the same code path the hook invokes. The HTTP
 * {@code @RequestBody} field-carrying contract is covered by
 * {@link SamplePatientEntryFormLabelBindingTest}; the null-guard /
 * fire-only-when-set SAFETY contract by
 * {@code SamplePatientEntryLabelsIntegrationTest}.
 *
 * <p>
 * Real DB ({@link BaseWebContextSensitiveTest}, {@code NOT_SUPPORTED} — writes
 * commit, so {@code @After} cleanup in FK order is mandatory). Legacy
 * {@code sample} / {@code sample_item} FK parents are seeded via
 * {@link JdbcTemplate} (numeric PKs), mirroring
 * {@code OrderLabelRequestSnapshotPersistenceTest}.
 */
public class SamplePatientEntryLabelRequestPersistenceTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "OLR-M5B-A";
    private static final String SPECIMEN_PRESET = "M5B-Specimen";
    private static final String ORDER_PRESET = "M5B-Order";
    private static final String TEST_ID_0 = "1";
    private static final String TEST_ID_1 = "2";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SamplePatientEntryService samplePatientEntryService;

    @Autowired
    private OrderLabelRequestDAO orderLabelRequestDAO;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestLabelPresetLinkService testLabelPresetLinkService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private TestService testService;

    private JdbcTemplate jdbc;

    private String sampleId;
    private String sampleItemId0;
    private String sampleItemId1;
    private LabelPreset specimenPreset;
    private LabelPreset orderPreset;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbc = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/test.xml");
        executeDataSetWithStateManagement("testdata/system-user.xml");
        cleanTestData();

        sampleId = String.valueOf(jdbc.queryForObject("SELECT nextval('sample_seq')", Long.class));
        jdbc.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated, sys_user_id) "
                        + "VALUES (?, ?, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)",
                Long.valueOf(sampleId), ACCESSION);

        sampleItemId0 = insertSampleItem(1);
        sampleItemId1 = insertSampleItem(2);

        // Per-sample preset linked to TEST_ID_1 (second positional sample) only,
        // so a per-position testIds map is observable: the per-sample cell for the
        // second sample resolves a "test" source/link; a globally-built map would
        // wrongly attach it to the first sample too.
        specimenPreset = savePreset(SPECIMEN_PRESET, true, false);
        orderPreset = savePreset(ORDER_PRESET, false, true);

        TestLabelPresetLink link = new TestLabelPresetLink();
        link.setTest(testService.getTestById(TEST_ID_1));
        link.setPreset(specimenPreset);
        link.setDefaultQty(2);
        link.setMaxQty(5);
        link.setAllowOverride(true);
        link.setSysUserId(TEST_SYS_USER_ID);
        testLabelPresetLinkService.insert(link);
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    /**
     * Drives the real save seam with a positional payload and proves rows land with
     * non-null snapshot, anchored to the correct sample items by position.
     */
    @Test
    public void persistLabelRequests_writesRows_correlatedPositionallyToRealSampleItems() {
        SamplePatientUpdateData updateData = buildUpdateDataWithTwoSamples();

        // Positional payload: one per-order cell + per-sample cells keyed "0"/"1".
        OrderLabelPersistRequest payload = new OrderLabelPersistRequest();
        payload.getOrderCells().add(new OrderLabelPersistRequest.PersistCell(orderPreset.getId(), 4));

        OrderLabelPersistRequest.PersistSampleRow row0 = new OrderLabelPersistRequest.PersistSampleRow("0");
        row0.getCells().add(new OrderLabelPersistRequest.PersistCell(specimenPreset.getId(), 1));
        payload.getSampleRows().add(row0);

        OrderLabelPersistRequest.PersistSampleRow row1 = new OrderLabelPersistRequest.PersistSampleRow("1");
        row1.getCells().add(new OrderLabelPersistRequest.PersistCell(specimenPreset.getId(), 3));
        payload.getSampleRows().add(row1);

        List<OrderLabelRequest> persisted = samplePatientEntryService.persistLabelRequests(updateData, payload,
                TEST_SYS_USER_ID);

        assertEquals("one per-order row + two per-sample rows", 3, persisted.size());

        // Re-read from the DB to prove the JSONB round-tripped through the typed
        // binding (not just the in-memory return value).
        List<OrderLabelRequest> rows = orderLabelRequestDAO.listByParentSampleId(sampleId);
        assertEquals(3, rows.size());

        // ── per-order row: no sample item, qty 4, non-null snapshot ──
        OrderLabelRequest perOrder = rows.stream().filter(r -> r.getSampleItem() == null).findFirst()
                .orElseThrow(() -> new AssertionError("missing per-order row"));
        assertEquals(Integer.valueOf(4), perOrder.getQty());
        assertNotNull("per-order preset_snapshot must be non-null", perOrder.getPresetSnapshot());
        assertEquals(orderPreset.getId(), perOrder.getPresetSnapshot().getPreset().getId());

        // ── per-sample rows: positional anchoring is the whole point ──
        OrderLabelRequest sampleRow0 = perSampleRowFor(rows, sampleItemId0);
        OrderLabelRequest sampleRow1 = perSampleRowFor(rows, sampleItemId1);

        assertEquals("sample_id_local \"0\" anchors to the first SampleTestCollection's item", Integer.valueOf(1),
                sampleRow0.getQty());
        assertEquals("sample_id_local \"1\" anchors to the second SampleTestCollection's item", Integer.valueOf(3),
                sampleRow1.getQty());

        PresetSnapshotDto snap0 = sampleRow0.getPresetSnapshot();
        PresetSnapshotDto snap1 = sampleRow1.getPresetSnapshot();
        assertNotNull("per-sample row 0 preset_snapshot must be non-null", snap0);
        assertNotNull("per-sample row 1 preset_snapshot must be non-null", snap1);

        // Sample 0 carries TEST_ID_0 (not linked to the preset) → no driving test
        // link. Sample 1 carries TEST_ID_1 (linked) → the test link is captured.
        // This proves testIdsBySampleLocal is built per position, not globally.
        assertNull("sample 0's test does not link the preset → no test_link in its snapshot", snap0.getTestLink());
        assertNotNull("sample 1's test links the preset → its snapshot captures the test_link", snap1.getTestLink());
        assertEquals(Integer.valueOf(Integer.parseInt(TEST_ID_1)), snap1.getTestLink().getTestId());
        assertEquals(Integer.valueOf(2), snap1.getTestLink().getDefaultQty());
        assertEquals(Integer.valueOf(5), snap1.getTestLink().getMaxQty());
    }

    /** A null payload must be a no-op (the save hook's guard relies on this). */
    @Test
    public void persistLabelRequests_nullPayload_writesNothing() {
        List<OrderLabelRequest> persisted = samplePatientEntryService
                .persistLabelRequests(buildUpdateDataWithTwoSamples(), null, TEST_SYS_USER_ID);
        org.junit.Assert.assertTrue("null payload persists no rows", persisted.isEmpty());
        org.junit.Assert.assertTrue("no order_label_request rows for a null payload",
                orderLabelRequestDAO.listByParentSampleId(sampleId).isEmpty());
    }

    private SamplePatientUpdateData buildUpdateDataWithTwoSamples() {
        Sample sample = sampleService.get(sampleId);
        assertNotNull("seeded sample must be loadable by id", sample);

        SampleItem item0 = sampleItemService.get(sampleItemId0);
        SampleItem item1 = sampleItemService.get(sampleItemId1);
        assertNotNull(item0);
        assertNotNull(item1);

        org.openelisglobal.test.valueholder.Test test0 = testService.getTestById(TEST_ID_0);
        org.openelisglobal.test.valueholder.Test test1 = testService.getTestById(TEST_ID_1);

        SamplePatientUpdateData updateData = new SamplePatientUpdateData(TEST_SYS_USER_ID);
        updateData.setSample(sample);

        // SampleTestCollection is a non-static inner class of SampleAddService;
        // construct via an outer instance. The fixture mirrors what persistData
        // leaves in updateData after a real save: ordered, ID-bearing items+tests.
        SampleAddService outer = new SampleAddService(null, TEST_SYS_USER_ID, sample, null);
        List<SampleTestCollection> stcs = new ArrayList<>();
        stcs.add(outer.new SampleTestCollection(item0, List.of(test0), null, null, null, null, null));
        stcs.add(outer.new SampleTestCollection(item1, List.of(test1), null, null, null, null, null));
        updateData.setSampleItemsTests(stcs);
        return updateData;
    }

    private OrderLabelRequest perSampleRowFor(List<OrderLabelRequest> rows, String sampleItemId) {
        return rows.stream().filter(r -> r.getSampleItem() != null && sampleItemId.equals(r.getSampleItem().getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("missing per-sample row for sample item " + sampleItemId));
    }

    private String insertSampleItem(int sortOrder) {
        String id = String.valueOf(jdbc.queryForObject("SELECT nextval('sample_item_seq')", Long.class));
        jdbc.update(
                "INSERT INTO sample_item (id, samp_id, sort_order, status_id, lastupdated) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                Long.valueOf(id), Long.valueOf(sampleId), sortOrder, 1);
        return id;
    }

    private LabelPreset savePreset(String name, boolean perSample, boolean perOrder) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        preset.setHeightMm(30);
        preset.setWidthMm(60);
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerSample(perSample);
        preset.setPrintsPerOrder(perOrder);
        preset.setDefaultPerSample(perSample ? 1 : 0);
        preset.setMaxPerSample(perSample ? 5 : 0);
        preset.setDefaultPerOrder(perOrder ? 1 : 0);
        preset.setMaxPerOrder(perOrder ? 10 : 0);
        preset.setIsSystem(false);
        preset.setIsActive(true);
        labelPresetDAO.insert(preset);
        return preset;
    }

    private void cleanTestData() {
        try {
            jdbc.update("DELETE FROM order_label_request WHERE parent_sample_id IN "
                    + "(SELECT id FROM sample WHERE accession_number = ?)", ACCESSION);
            jdbc.update("DELETE FROM order_label_request WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'M5B-%')");
            jdbc.update("DELETE FROM test_label_preset_link WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'M5B-%')");
            jdbc.update(
                    "DELETE FROM sample_item WHERE samp_id IN " + "(SELECT id FROM sample WHERE accession_number = ?)",
                    ACCESSION);
            jdbc.update("DELETE FROM sample WHERE accession_number = ?", ACCESSION);
            jdbc.update("DELETE FROM label_preset WHERE name LIKE 'M5B-%'");
        } catch (Exception e) {
            // ignored — next run retries
        }
    }
}
