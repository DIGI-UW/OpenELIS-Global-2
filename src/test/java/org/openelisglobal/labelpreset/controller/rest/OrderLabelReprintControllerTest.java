package org.openelisglobal.labelpreset.controller.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dto.OrderLabelPersistRequest;
import org.openelisglobal.labelpreset.service.OrderLabelRequestService;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration test for {@link OrderLabelReprintController} (OGC-285 M6,
 * T168/T169). Real Spring stack — controller + service + DAO + PostgreSQL via
 * {@link BaseWebContextSensitiveTest} and MockMvc.
 *
 * <p>
 * {@code labelpreset.controller.rest} is in the test component scan (it is NOT
 * excluded in {@code AppTestConfig}), and {@code BaseWebContextSensitiveTest}
 * seeds a {@code ROLE_ADMIN} security context, so the controller's
 * {@code @PreAuthorize("hasRole('ADMIN')")} is satisfied.
 */
public class OrderLabelReprintControllerTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION = "OLR-T168-A";
    private static final String SPECIMEN_PRESET = "T168-CtrlSpecimen";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrderLabelRequestService orderLabelRequestService;

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
        row.getCells().add(new OrderLabelPersistRequest.PersistCell(preset.getId(), 2));
        payload.getSampleRows().add(row);
        orderLabelRequestService.persistRequest(sampleId, Map.of("S1", sampleItemId), payload, TEST_SYS_USER_ID,
                Map.of("S1", List.of()));
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    @Test
    public void getOrderLabels_returnsRowsWithFrozenSnapshot() throws Exception {
        mockMvc.perform(get("/api/orders/{id}/labels", sampleId).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].parent_sample_id").value(sampleId))
                .andExpect(jsonPath("$[0].sample_item_id").value(sampleItemId))
                .andExpect(jsonPath("$[0].preset_id").value(preset.getId())).andExpect(jsonPath("$[0].qty").value(2))
                // frozen snapshot is exposed verbatim (snake_case nested shape)
                .andExpect(jsonPath("$[0].preset_snapshot.preset.id").value(preset.getId()))
                .andExpect(jsonPath("$[0].preset_snapshot.preset.height_mm").value(25))
                .andExpect(jsonPath("$[0].preset_snapshot.preset.barcode_type").value("CODE_128"));
    }

    @Test
    public void getOrderLabelsByAccession_resolvesAccessionToSameRows() throws Exception {
        // Frontends hold the accession, not the internal Sample PK. The
        // accession-keyed path must surface the identical rows as the PK path.
        mockMvc.perform(
                get("/api/orders/by-accession/{accessionNumber}/labels", ACCESSION).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].parent_sample_id").value(sampleId))
                .andExpect(jsonPath("$[0].sample_item_id").value(sampleItemId))
                .andExpect(jsonPath("$[0].preset_id").value(preset.getId())).andExpect(jsonPath("$[0].qty").value(2))
                .andExpect(jsonPath("$[0].preset_snapshot.preset.id").value(preset.getId()))
                .andExpect(jsonPath("$[0].preset_snapshot.preset.height_mm").value(25))
                .andExpect(jsonPath("$[0].preset_snapshot.preset.barcode_type").value("CODE_128"));
    }

    @Test
    public void getOrderLabelsByAccession_unknownAccession_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/by-accession/{accessionNumber}/labels", "OLR-T168-DOES-NOT-EXIST")
                .accept(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    public void printFromSnapshot_returnsApplicationPdf() throws Exception {
        mockMvc.perform(get("/api/barcode/print/{orderId}/{presetId}", sampleId, preset.getId()))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF));
    }

    @Test
    public void printFromSnapshot_noRows_returns404() throws Exception {
        // A preset id with no persisted rows for this order yields an empty render.
        mockMvc.perform(get("/api/barcode/print/{orderId}/{presetId}", sampleId, 999999))
                .andExpect(status().isNotFound());
    }

    private void cleanTestData() {
        try {
            jdbc.update("DELETE FROM order_label_request WHERE parent_sample_id IN "
                    + "(SELECT id FROM sample WHERE accession_number = ?)", ACCESSION);
            jdbc.update("DELETE FROM order_label_request WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name LIKE 'T168-%')");
            jdbc.update(
                    "DELETE FROM sample_item WHERE samp_id IN " + "(SELECT id FROM sample WHERE accession_number = ?)",
                    ACCESSION);
            jdbc.update("DELETE FROM sample WHERE accession_number = ?", ACCESSION);
            jdbc.update("DELETE FROM label_preset WHERE name LIKE 'T168-%'");
        } catch (Exception e) {
            // ignored — next run retries
        }
    }
}
