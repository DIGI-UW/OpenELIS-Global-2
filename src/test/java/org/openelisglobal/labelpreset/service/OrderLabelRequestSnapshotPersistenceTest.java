package org.openelisglobal.labelpreset.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.OrderLabelRequestDAO;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.test.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Persistence test for {@link OrderLabelRequestServiceImpl#persistRequest}
 * (OGC-285 M5). Saves a candidate order's chosen quantities and asserts
 * {@code order_label_request} rows are created with a non-null
 * {@code preset_snapshot} matching the FRS §7.3.1 shape, serialized for real
 * (typed JSONB binding via {@code JsonbObjectType} — NOT mocked Jackson).
 *
 * <p>
 * Real DB ({@link BaseWebContextSensitiveTest}, {@code NOT_SUPPORTED} — writes
 * commit, so {@code @After} cleanup in FK order is mandatory). Legacy
 * {@code sample} / {@code sample_item} FK parents are seeded via
 * {@link JdbcTemplate} (numeric PKs), mirroring
 * {@code OrderLabelRequestDAOImplTest}. The custom {@link LabelPreset} is
 * persisted via its real DAO.
 */
public class OrderLabelRequestSnapshotPersistenceTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "OLR-T134-A";
    private static final String SPECIMEN_PRESET = "T134-PersistSpecimen";
    private static final String ORDER_PRESET = "T134-PersistOrder";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrderLabelRequestService orderLabelRequestService;

    @Autowired
    private OrderLabelRequestDAO orderLabelRequestDAO;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestLabelPresetLinkService testLabelPresetLinkService;

    @Autowired
    private TestService testService;

    private JdbcTemplate jdbc;

    private String sampleId;
    private String sampleItemId;
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

        sampleItemId = String.valueOf(jdbc.queryForObject("SELECT nextval('sample_item_seq')", Long.class));
        jdbc.update(
                "INSERT INTO sample_item (id, samp_id, sort_order, status_id, lastupdated) "
                        + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                Long.valueOf(sampleItemId), Long.valueOf(sampleId), 1, 1);

        // Per-sample preset linked to CBC (test id=1); per-order preset (unlinked).
        specimenPreset = savePreset(SPECIMEN_PRESET, true, false, 1, 5);
        orderPreset = savePreset(ORDER_PRESET, false, true, 0, 10);

        TestLabelPresetLink link = new TestLabelPresetLink();
        link.setTest(testService.getTestById("1"));
        link.setPreset(specimenPreset);
        link.setDefaultQty(1);
        link.setMaxQty(5);
        link.setAllowOverride(true);
        link.setSysUserId(TEST_SYS_USER_ID);
        testLabelPresetLinkService.insert(link);
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    @Test
    public void persistRequest_writesRowsWithNonNullSnapshotMatchingFrsShape() {
        OrderLabelPersistRequest payload = new OrderLabelPersistRequest();
        // one per-order cell (qty 2) + one per-sample cell (qty 3)
        payload.getOrderCells().add(new OrderLabelPersistRequest.PersistCell(orderPreset.getId(), 2));
        OrderLabelPersistRequest.PersistSampleRow row = new OrderLabelPersistRequest.PersistSampleRow("S1");
        row.getCells().add(new OrderLabelPersistRequest.PersistCell(specimenPreset.getId(), 3));
        payload.getSampleRows().add(row);

        List<OrderLabelRequest> persisted = orderLabelRequestService.persistRequest(sampleId,
                Map.of("S1", sampleItemId), payload, TEST_SYS_USER_ID, Map.of("S1", List.of("1")));

        assertEquals("one per-order row + one per-sample row", 2, persisted.size());

        // Re-read from the DB (not the in-memory objects) to prove the JSONB
        // round-tripped through the typed binding.
        List<OrderLabelRequest> rows = orderLabelRequestDAO.listByParentSampleId(sampleId);
        assertEquals(2, rows.size());

        OrderLabelRequest perOrder = rows.stream().filter(r -> orderPreset.getId().equals(r.getPreset().getId()))
                .findFirst().orElseThrow(() -> new AssertionError("missing per-order row"));
        OrderLabelRequest perSample = rows.stream().filter(r -> specimenPreset.getId().equals(r.getPreset().getId()))
                .findFirst().orElseThrow(() -> new AssertionError("missing per-sample row"));

        // ── per-order row: sampleItem null, qty 2, snapshot present, NO test_link ──
        assertNull("per-order row has no sample item", perOrder.getSampleItem());
        assertEquals(Integer.valueOf(2), perOrder.getQty());
        PresetSnapshotDto orderSnap = perOrder.getPresetSnapshot();
        assertNotNull("per-order preset_snapshot must be non-null", orderSnap);
        assertNotNull(orderSnap.getPreset());
        assertEquals(orderPreset.getId(), orderSnap.getPreset().getId());
        assertEquals(ORDER_PRESET, orderSnap.getPreset().getName());
        assertEquals(BarcodeType.CODE_128.name(), orderSnap.getPreset().getBarcodeType());
        assertNull("per-order cell has no driving test link", orderSnap.getTestLink());

        // ── per-sample row: bound to the sample item, qty 3, snapshot + test_link ──
        assertNotNull("per-sample row carries its sample item", perSample.getSampleItem());
        assertEquals(sampleItemId, perSample.getSampleItem().getId());
        assertEquals(Integer.valueOf(3), perSample.getQty());
        PresetSnapshotDto sampleSnap = perSample.getPresetSnapshot();
        assertNotNull("per-sample preset_snapshot must be non-null", sampleSnap);
        assertNotNull(sampleSnap.getPreset());
        assertEquals(specimenPreset.getId(), sampleSnap.getPreset().getId());
        assertEquals(SPECIMEN_PRESET, sampleSnap.getPreset().getName());
        // test_link captured from the linking test (CBC id=1)
        assertNotNull("per-sample snapshot must capture the driving test link", sampleSnap.getTestLink());
        assertEquals(Integer.valueOf(1), sampleSnap.getTestLink().getTestId());
        assertEquals(Integer.valueOf(1), sampleSnap.getTestLink().getDefaultQty());
        assertEquals(Integer.valueOf(5), sampleSnap.getTestLink().getMaxQty());
        assertTrue(sampleSnap.getTestLink().getAllowOverride());
    }

    @Test
    public void persistRequest_skipsZeroQtyCells() {
        OrderLabelPersistRequest payload = new OrderLabelPersistRequest();
        payload.getOrderCells().add(new OrderLabelPersistRequest.PersistCell(orderPreset.getId(), 0));
        OrderLabelPersistRequest.PersistSampleRow row = new OrderLabelPersistRequest.PersistSampleRow("S1");
        row.getCells().add(new OrderLabelPersistRequest.PersistCell(specimenPreset.getId(), 0));
        payload.getSampleRows().add(row);

        List<OrderLabelRequest> persisted = orderLabelRequestService.persistRequest(sampleId,
                Map.of("S1", sampleItemId), payload, TEST_SYS_USER_ID, Map.of());

        assertTrue("zero-qty cells are not persisted", persisted.isEmpty());
        assertTrue("no rows in DB for an all-zero payload",
                orderLabelRequestDAO.listByParentSampleId(sampleId).isEmpty());
    }

    @Test
    public void persistRequest_skipsSampleRowWithUnmappedLocalId() {
        OrderLabelPersistRequest payload = new OrderLabelPersistRequest();
        OrderLabelPersistRequest.PersistSampleRow row = new OrderLabelPersistRequest.PersistSampleRow("UNKNOWN");
        row.getCells().add(new OrderLabelPersistRequest.PersistCell(specimenPreset.getId(), 3));
        payload.getSampleRows().add(row);

        List<OrderLabelRequest> persisted = orderLabelRequestService.persistRequest(sampleId,
                Map.of("S1", sampleItemId), payload, TEST_SYS_USER_ID, Map.of());

        assertTrue("a sample row whose local id is not in the id map is skipped", persisted.isEmpty());
        assertTrue("no order_label_request rows written for an unmapped-only payload",
                orderLabelRequestDAO.listByParentSampleId(sampleId).isEmpty());
    }

    private LabelPreset savePreset(String name, boolean perSample, boolean perOrder, int defaultPerSample,
            int maxPerSample) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        preset.setHeightMm(30);
        preset.setWidthMm(60);
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerSample(perSample);
        preset.setPrintsPerOrder(perOrder);
        preset.setDefaultPerSample(defaultPerSample);
        preset.setMaxPerSample(maxPerSample);
        preset.setDefaultPerOrder(perOrder ? 1 : 0);
        preset.setMaxPerOrder(10);
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
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T134-%')");
            jdbc.update("DELETE FROM test_label_preset_link WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T134-%')");
            jdbc.update(
                    "DELETE FROM sample_item WHERE samp_id IN " + "(SELECT id FROM sample WHERE accession_number = ?)",
                    ACCESSION);
            jdbc.update("DELETE FROM sample WHERE accession_number = ?", ACCESSION);
            jdbc.update("DELETE FROM label_preset WHERE name LIKE 'T134-%'");
        } catch (Exception e) {
            // ignored — next run retries
        }
    }
}
