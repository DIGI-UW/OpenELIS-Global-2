package org.openelisglobal.labelpreset.valueholder;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Arrays;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.OrderLabelRequestDAO;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto.PresetSnapshotField;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto.PresetSnapshotPreset;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto.PresetSnapshotTestLink;
import org.openelisglobal.sample.dao.SampleDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JSONB round-trip test for {@code order_label_request.preset_snapshot}
 * (data-model.md §4 / FRS §7.3.1). Persists an {@link OrderLabelRequest} whose
 * {@link PresetSnapshotDto} is fully populated (preset + ordered fields +
 * test_link), then re-reads it through the real DAO + real Hibernate
 * {@link org.openelisglobal.hibernate.type.JsonBinaryType} UserType and asserts
 * every nested value survives the round-trip.
 *
 * <p>
 * Inversion-worthy: the assertions read back through
 * {@code getPresetSnapshot()} (so they exercise the
 * {@code @Type(type = "jsonb")} binding — a raw JDBC String read would not),
 * and a JSONB-path SQL probe asserts the column holds real JSON (so a binding
 * that wrote {@code Object.toString()} garbage instead of serialized JSON fails
 * loudly rather than silently). If the {@code @Type(type = "jsonb")} binding
 * were dropped, or the nested mapping mangled, this test fails.
 *
 * <p>
 * Lifecycle mirrors {@code PatientMergeAuditDAOTest} (the existing
 * JsonBinaryType DB test): {@code BaseWebContextSensitiveTest} runs with
 * {@code @Transactional(NOT_SUPPORTED)}, so each {@code dao.insert} /
 * {@code dao.get} call is its own transaction + session — the {@code get} is a
 * genuine fresh-from-DB read, not a first-level-cache hit.
 */
public class PresetSnapshotJsonbRoundtripTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrderLabelRequestDAO orderLabelRequestDAO;

    @Autowired
    private SampleDAO sampleDAO;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    private JdbcTemplate jdbcTemplate;

    private String sampleId;
    private Integer presetId;
    private Integer orderLabelRequestId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        sampleId = createSampleRow();
        presetId = createLabelPresetRow();
    }

    @After
    public void tearDown() {
        if (orderLabelRequestId != null) {
            jdbcTemplate.update("DELETE FROM clinlims.order_label_request WHERE id = ?", orderLabelRequestId);
        }
        if (presetId != null) {
            jdbcTemplate.update("DELETE FROM clinlims.label_preset WHERE id = ?", presetId);
        }
        if (sampleId != null) {
            jdbcTemplate.update("DELETE FROM clinlims.sample WHERE id = ?", Long.valueOf(sampleId));
        }
    }

    @Test
    public void presetSnapshotJsonbRoundTripsAllNestedValues() {
        // Load the FK parents so they carry version/snapshot state. A bare
        // `new Sample(); setId()` is treated as transient (Sample.hbm.xml maps a
        // <version> column) and Hibernate refuses the not-null FK write; a loaded
        // parent is treated as existing and the FK is written from its id.
        OrderLabelRequest request = new OrderLabelRequest();
        request.setParentSample(sampleDAO.get(sampleId).orElseThrow());
        request.setPreset(labelPresetDAO.get(presetId).orElseThrow());
        request.setQty(2);
        request.setPresetSnapshot(buildFullSnapshot(presetId));
        request.setSysUserId(TEST_SYS_USER_ID);

        orderLabelRequestId = orderLabelRequestDAO.insert(request);
        assertNotNull("Inserted order_label_request should have a generated id", orderLabelRequestId);

        // Fresh read through the DAO -> exercises the @Type(type="jsonb") binding.
        OrderLabelRequest reloaded = orderLabelRequestDAO.get(orderLabelRequestId).orElse(null);
        assertNotNull("Should re-read the persisted order_label_request", reloaded);

        PresetSnapshotDto snapshot = reloaded.getPresetSnapshot();
        assertNotNull("preset_snapshot must deserialize from JSONB, not come back null", snapshot);

        // --- preset{} block ---
        PresetSnapshotPreset preset = snapshot.getPreset();
        assertNotNull("snapshot.preset must round-trip", preset);
        assertEquals("preset.id", presetId, preset.getId());
        assertEquals("preset.name", "Specimen Label", preset.getName());
        assertEquals("preset.height_mm", Integer.valueOf(25), preset.getHeightMm());
        assertEquals("preset.width_mm", Integer.valueOf(76), preset.getWidthMm());
        assertEquals("preset.barcode_type", "CODE_128", preset.getBarcodeType());

        // --- fields[] block, order preserved ---
        assertNotNull("snapshot.fields must round-trip", snapshot.getFields());
        assertEquals("snapshot.fields size", 3, snapshot.getFields().size());

        PresetSnapshotField f0 = snapshot.getFields().get(0);
        assertEquals("fields[0].field_key", "LAB_NUMBER", f0.getFieldKey());
        assertEquals("fields[0].field_label", "Lab Number", f0.getFieldLabel());
        assertEquals("fields[0].is_required", Boolean.TRUE, f0.getIsRequired());
        assertEquals("fields[0].display_order", Integer.valueOf(1), f0.getDisplayOrder());

        PresetSnapshotField f1 = snapshot.getFields().get(1);
        assertEquals("fields[1].field_key", "PATIENT_NAME", f1.getFieldKey());
        assertEquals("fields[1].field_label", "Patient Name", f1.getFieldLabel());
        assertEquals("fields[1].is_required", Boolean.FALSE, f1.getIsRequired());
        assertEquals("fields[1].display_order", Integer.valueOf(2), f1.getDisplayOrder());

        PresetSnapshotField f2 = snapshot.getFields().get(2);
        assertEquals("fields[2].field_key", "COLLECTION_DATETIME", f2.getFieldKey());
        assertEquals("fields[2].field_label", "Collection Date/Time", f2.getFieldLabel());
        assertEquals("fields[2].is_required", Boolean.FALSE, f2.getIsRequired());
        assertEquals("fields[2].display_order", Integer.valueOf(3), f2.getDisplayOrder());

        // --- test_link{} block ---
        PresetSnapshotTestLink testLink = snapshot.getTestLink();
        assertNotNull("snapshot.test_link must round-trip", testLink);
        assertEquals("test_link.test_id", Integer.valueOf(412), testLink.getTestId());
        assertEquals("test_link.default_qty", Integer.valueOf(2), testLink.getDefaultQty());
        assertEquals("test_link.max_qty", Integer.valueOf(5), testLink.getMaxQty());
        assertEquals("test_link.allow_override", Boolean.TRUE, testLink.getAllowOverride());

        // Defense-in-depth: the column must contain real JSON (a binding that wrote
        // PresetSnapshotDto.toString() would store an object-identity string, which
        // these jsonb-path queries cannot navigate). Proves serialization, not just
        // that some bytes came back.
        String presetNameViaJsonbPath = jdbcTemplate.queryForObject(
                "SELECT preset_snapshot -> 'preset' ->> 'name' FROM clinlims.order_label_request WHERE id = ?",
                String.class, orderLabelRequestId);
        assertEquals("JSONB path preset.name", "Specimen Label", presetNameViaJsonbPath);

        Integer fieldCountViaJsonbPath = jdbcTemplate.queryForObject(
                "SELECT jsonb_array_length(preset_snapshot -> 'fields') FROM clinlims.order_label_request WHERE id = ?",
                Integer.class, orderLabelRequestId);
        assertEquals("JSONB path fields length", Integer.valueOf(3), fieldCountViaJsonbPath);

        String testLinkTestIdViaJsonbPath = jdbcTemplate.queryForObject(
                "SELECT preset_snapshot -> 'test_link' ->> 'test_id' FROM clinlims.order_label_request WHERE id = ?",
                String.class, orderLabelRequestId);
        assertEquals("JSONB path test_link.test_id", "412", testLinkTestIdViaJsonbPath);
    }

    private PresetSnapshotDto buildFullSnapshot(Integer presetId) {
        PresetSnapshotPreset preset = new PresetSnapshotPreset();
        preset.setId(presetId);
        preset.setName("Specimen Label");
        preset.setHeightMm(25);
        preset.setWidthMm(76);
        preset.setBarcodeType("CODE_128");

        PresetSnapshotField labNumber = new PresetSnapshotField();
        labNumber.setFieldKey("LAB_NUMBER");
        labNumber.setFieldLabel("Lab Number");
        labNumber.setIsRequired(true);
        labNumber.setDisplayOrder(1);

        PresetSnapshotField patientName = new PresetSnapshotField();
        patientName.setFieldKey("PATIENT_NAME");
        patientName.setFieldLabel("Patient Name");
        patientName.setIsRequired(false);
        patientName.setDisplayOrder(2);

        PresetSnapshotField collection = new PresetSnapshotField();
        collection.setFieldKey("COLLECTION_DATETIME");
        collection.setFieldLabel("Collection Date/Time");
        collection.setIsRequired(false);
        collection.setDisplayOrder(3);

        PresetSnapshotTestLink testLink = new PresetSnapshotTestLink();
        testLink.setTestId(412);
        testLink.setDefaultQty(2);
        testLink.setMaxQty(5);
        testLink.setAllowOverride(true);

        PresetSnapshotDto snapshot = new PresetSnapshotDto();
        snapshot.setPreset(preset);
        snapshot.setFields(Arrays.asList(labNumber, patientName, collection));
        snapshot.setTestLink(testLink);
        return snapshot;
    }

    private String createSampleRow() {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, is_confirmation,"
                        + " lastupdated) VALUES (nextval('clinlims.sample_seq'), ?, CURRENT_TIMESTAMP,"
                        + " CURRENT_TIMESTAMP, false, CURRENT_TIMESTAMP) RETURNING id",
                Long.class, "JSONB-RT-" + System.currentTimeMillis() % 1000000);
        return String.valueOf(id);
    }

    private Integer createLabelPresetRow() {
        return jdbcTemplate.queryForObject(
                "INSERT INTO clinlims.label_preset (id, name, height_mm, width_mm, barcode_type, prints_per_order,"
                        + " prints_per_sample, default_per_order, max_per_order, default_per_sample, max_per_sample,"
                        + " is_system, is_active, last_updated) VALUES (nextval('clinlims.label_preset_seq'), ?, 25, 76,"
                        + " 'CODE_128', false, true, 0, 10, 1, 10, false, true, CURRENT_TIMESTAMP) RETURNING id",
                Integer.class, "JSONB Round-trip Preset " + new Timestamp(System.currentTimeMillis()).getTime());
    }
}
