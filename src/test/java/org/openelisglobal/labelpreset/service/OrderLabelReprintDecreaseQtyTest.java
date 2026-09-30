package org.openelisglobal.labelpreset.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * T170 — decrease-only quantity guard for
 * {@link OrderLabelReprintService#decreaseQty}. A print action may lower the
 * saved {@code qty} (or leave it unchanged) but must never raise it above the
 * quantity frozen at order-save time. Real DB
 * ({@link BaseWebContextSensitiveTest}).
 */
public class OrderLabelReprintDecreaseQtyTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "OLR-T170-A";
    private static final String SPECIMEN_PRESET = "T170-DecSpecimen";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrderLabelRequestService orderLabelRequestService;

    @Autowired
    private OrderLabelRequestDAO orderLabelRequestDAO;

    @Autowired
    private OrderLabelReprintService orderLabelReprintService;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    private JdbcTemplate jdbc;
    private String sampleId;
    private String sampleItemId;
    private LabelPreset preset;
    private Integer requestId;

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

        preset = new LabelPreset();
        preset.setName(SPECIMEN_PRESET);
        preset.setHeightMm(25);
        preset.setWidthMm(50);
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerOrder(false);
        preset.setPrintsPerSample(true);
        preset.setDefaultPerOrder(0);
        preset.setMaxPerOrder(10);
        preset.setDefaultPerSample(1);
        preset.setMaxPerSample(5);
        preset.setIsSystem(false);
        preset.setIsActive(true);
        labelPresetDAO.insert(preset);

        OrderLabelPersistRequest payload = new OrderLabelPersistRequest();
        OrderLabelPersistRequest.PersistSampleRow row = new OrderLabelPersistRequest.PersistSampleRow("S1");
        row.getCells().add(new OrderLabelPersistRequest.PersistCell(preset.getId(), 3));
        payload.getSampleRows().add(row);
        List<OrderLabelRequest> persisted = orderLabelRequestService.persistRequest(sampleId,
                Map.of("S1", sampleItemId), payload, TEST_SYS_USER_ID, Map.of("S1", List.of()));
        requestId = persisted.get(0).getId();
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    @Test
    public void decreaseQty_allowsLowering() {
        OrderLabelRequest updated = orderLabelReprintService.decreaseQty(requestId, 1);
        assertEquals("qty lowered from 3 to 1", Integer.valueOf(1), updated.getQty());

        // Persisted, not just in-memory.
        OrderLabelRequest reread = orderLabelRequestDAO.get(requestId).orElseThrow();
        assertEquals(Integer.valueOf(1), reread.getQty());
    }

    @Test
    public void decreaseQty_allowsEqual() {
        OrderLabelRequest updated = orderLabelReprintService.decreaseQty(requestId, 3);
        assertEquals("equal qty is permitted", Integer.valueOf(3), updated.getQty());
    }

    @Test
    public void decreaseQty_rejectsIncreaseAboveSaved() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> orderLabelReprintService.decreaseQty(requestId, 4));
        assertEquals(true, ex.getMessage().contains("decreased"));

        // The saved qty is untouched.
        OrderLabelRequest reread = orderLabelRequestDAO.get(requestId).orElseThrow();
        assertEquals("rejected increase leaves saved qty at 3", Integer.valueOf(3), reread.getQty());
    }

    private void cleanTestData() {
        try {
            jdbc.update("DELETE FROM order_label_request WHERE parent_sample_id IN "
                    + "(SELECT id FROM sample WHERE accession_number = ?)", ACCESSION);
            jdbc.update("DELETE FROM order_label_request WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T170-%')");
            jdbc.update(
                    "DELETE FROM sample_item WHERE samp_id IN " + "(SELECT id FROM sample WHERE accession_number = ?)",
                    ACCESSION);
            jdbc.update("DELETE FROM sample WHERE accession_number = ?", ACCESSION);
            jdbc.update("DELETE FROM label_preset WHERE name LIKE 'T170-%'");
        } catch (Exception e) {
            // ignored — next run retries
        }
    }
}
