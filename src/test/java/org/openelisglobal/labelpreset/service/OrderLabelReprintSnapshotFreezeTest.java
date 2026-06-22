package org.openelisglobal.labelpreset.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.barcode.labeltype.SnapshotLabel;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.OrderLabelRequestDAO;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * AC-20 regression test (OGC-285 M6) — the HARD-RULE proof that reprint renders
 * from the frozen {@code preset_snapshot}, not the live {@code label_preset}.
 *
 * <p>
 * Scenario: create a preset with {@code height_mm = 25} → persist an
 * {@code order_label_request} whose snapshot froze 25 → mutate the live preset
 * to {@code height_mm = 50} directly in the DB → re-read the row from the DB →
 * render via {@link OrderLabelReprintService}. The label frame and the rendered
 * PDF page must reflect the snapshot's 25, NOT the mutated 50.
 *
 * <p>
 * Math: width is held at 50; the maker hardcodes {@code pdfWidth = 350} and
 * derives {@code pdfHeight = 350 * (height/width)}. Snapshot 25/50 → page
 * height 175; the bug (live 50/50) → page height 350. Distinct and observable
 * through the production {@code createLabelsAsStream} path.
 *
 * <p>
 * Real DB ({@link BaseWebContextSensitiveTest}, {@code NOT_SUPPORTED} — writes
 * commit, so {@code @After} cleanup in FK order is mandatory). Legacy
 * {@code sample}/{@code sample_item} FK parents are seeded via
 * {@link JdbcTemplate}; the {@link LabelPreset} is persisted via its real DAO
 * and the snapshot via the real persistence service.
 */
public class OrderLabelReprintSnapshotFreezeTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "OLR-T163-A";
    private static final String SPECIMEN_PRESET = "T163-FreezeSpecimen";

    private static final float SNAPSHOT_HEIGHT_MM = 25f;
    private static final float MUTATED_HEIGHT_MM = 50f;
    private static final float WIDTH_MM = 50f;
    // pdfWidth (350) * (height/width). Snapshot path: 350 * (25/50) = 175.
    private static final float EXPECTED_PAGE_HEIGHT = 175f;
    private static final float BUGGY_PAGE_HEIGHT = 350f;

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

        // Preset frozen at height 25, width 50.
        preset = new LabelPreset();
        preset.setName(SPECIMEN_PRESET);
        preset.setHeightMm((int) SNAPSHOT_HEIGHT_MM);
        preset.setWidthMm((int) WIDTH_MM);
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
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    @Test
    public void reprintRendersFromSnapshotHeight_notMutatedLivePreset() throws Exception {
        // 1) Persist an order_label_request whose snapshot freezes height 25.
        OrderLabelPersistRequest payload = new OrderLabelPersistRequest();
        OrderLabelPersistRequest.PersistSampleRow row = new OrderLabelPersistRequest.PersistSampleRow("S1");
        row.getCells().add(new OrderLabelPersistRequest.PersistCell(preset.getId(), 1));
        payload.getSampleRows().add(row);

        List<OrderLabelRequest> persisted = orderLabelRequestService.persistRequest(sampleId,
                Map.of("S1", sampleItemId), payload, TEST_SYS_USER_ID, Map.of("S1", List.of()));
        assertEquals("one per-sample row persisted", 1, persisted.size());

        // Sanity: the snapshot on disk froze 25.
        Integer snapHeightAtSave = persisted.get(0).getPresetSnapshot().getPreset().getHeightMm();
        assertEquals("snapshot froze the height at save time", Integer.valueOf((int) SNAPSHOT_HEIGHT_MM),
                snapHeightAtSave);

        // 2) Mutate the LIVE preset height 25 -> 50 via the DAO (the exact path the
        // hard rule names). If reprint re-fetches label_preset, it will now see 50
        // and the assertions below fail. LabelPreset has no L2 cache, so a re-fetch
        // would hit the mutated row — making the dynamic assert below catch the
        // named "reprint re-reads the live preset" bug directly, not by proxy.
        LabelPreset live = labelPresetDAO.get(preset.getId()).orElseThrow();
        live.setHeightMm((int) MUTATED_HEIGHT_MM);
        live.setSysUserId(TEST_SYS_USER_ID);
        labelPresetDAO.update(live);
        Integer liveHeightNow = jdbc.queryForObject("SELECT height_mm FROM label_preset WHERE id = ?", Integer.class,
                preset.getId());
        assertEquals("live preset row was mutated to 50 via DAO", Integer.valueOf((int) MUTATED_HEIGHT_MM),
                liveHeightNow);

        // 3) Re-read the row fresh from the DB (prove the JSONB on disk froze 25,
        // not the in-memory DTO we built at save time).
        List<OrderLabelRequest> rows = orderLabelRequestDAO.listByParentSampleId(sampleId);
        assertEquals(1, rows.size());
        PresetSnapshotDto reread = rows.get(0).getPresetSnapshot();
        assertNotNull(reread);
        assertNotNull(reread.getPreset());
        assertEquals("re-read snapshot still 25 after live mutation to 50", Integer.valueOf((int) SNAPSHOT_HEIGHT_MM),
                reread.getPreset().getHeightMm());

        // 4a) Label-level discriminator: the frame dimension comes from the snapshot.
        SnapshotLabel label = orderLabelReprintService.buildSnapshotLabel(reread, ACCESSION + ".1");
        assertEquals("SnapshotLabel height is the snapshot's 25, not the live 50", SNAPSHOT_HEIGHT_MM,
                label.getHeight(), 0.001f);
        assertEquals("SnapshotLabel width unchanged", WIDTH_MM, label.getWidth(), 0.001f);

        // 4b) Output-level proof: render through the production path and read the
        // PDF page height. Snapshot 25/50 -> 175; the bug (live 50) -> 350.
        ByteArrayOutputStream pdf = orderLabelReprintService.renderFromSnapshot(sampleId, preset.getId());
        assertTrue("reprint produced a non-empty PDF", pdf.size() > 0);

        PdfReader pdfReader = new PdfReader(pdf.toByteArray());
        try {
            assertEquals("PDF has one label page", 1, pdfReader.getNumberOfPages());
            Rectangle pageSize = pdfReader.getPageSize(1);
            assertEquals("rendered page height reflects the SNAPSHOT's 25 (page height 175), not the live 50 (350)",
                    EXPECTED_PAGE_HEIGHT, pageSize.getHeight(), 1.0f);
            assertTrue("page height must NOT be the buggy live-preset value (350)",
                    Math.abs(pageSize.getHeight() - BUGGY_PAGE_HEIGHT) > 1.0f);
        } finally {
            pdfReader.close();
        }
    }

    private void cleanTestData() {
        try {
            jdbc.update("DELETE FROM order_label_request WHERE parent_sample_id IN "
                    + "(SELECT id FROM sample WHERE accession_number = ?)", ACCESSION);
            jdbc.update("DELETE FROM order_label_request WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T163-%')");
            jdbc.update("DELETE FROM test_label_preset_link WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T163-%')");
            jdbc.update(
                    "DELETE FROM sample_item WHERE samp_id IN " + "(SELECT id FROM sample WHERE accession_number = ?)",
                    ACCESSION);
            jdbc.update("DELETE FROM sample WHERE accession_number = ?", ACCESSION);
            jdbc.update("DELETE FROM label_preset WHERE name LIKE 'T163-%'");
        } catch (Exception e) {
            // ignored — next run retries
        }
    }
}
