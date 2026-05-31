package org.openelisglobal.labelpreset.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.OrderLabelRequest;
import org.openelisglobal.labelpreset.valueholder.PresetSnapshotDto;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO tests for {@link OrderLabelRequestDAO} (T029).
 *
 * <p>
 * Exercises the real {@link OrderLabelRequestDAOImpl} bean against the real
 * test database (no Mockito of the DAO, entity, or {@code JsonBinaryType} —
 * anti-mocking discipline per Constitution V.6). FK parents are seeded for
 * real: the two {@link LabelPreset} rows are persisted through their own real
 * DAO (clean {@code Integer} entity), and the legacy {@code sample} /
 * {@code sample_item} parents are inserted via {@link JdbcTemplate} (they are
 * pre-Jakarta {@code numeric(10,0)} tables — see data-model.md §"OE
 * legacy-vs-new ID convention"). Column sets for those inserts mirror the
 * known-good rows in {@code testdata/sample-storage-integration-test-data.xml}.
 *
 * <p>
 * Scope: the three finders that actually exist on the interface —
 * {@code listByParentSampleId}, {@code listBySampleItemId},
 * {@code listByPresetId}. The deep JSONB round-trip of {@code preset_snapshot}
 * is covered separately by {@code PresetSnapshotJsonbRoundtripTest} (T026);
 * here the snapshot is populated only so the NOT-NULL {@code preset_snapshot}
 * column is satisfied and a row is actually persisted for the finders to
 * return.
 *
 * <p>
 * Inversion-worthiness: the fixture seeds <em>discriminating</em> rows — two
 * parent samples, two presets, one per-order row ({@code sampleItem == null})
 * and two per-sample rows — and every finder asserts it returns <em>only</em>
 * its matching subset (both inclusion AND exclusion). A finder whose
 * {@code WHERE} clause was dropped (returning every row) would fail these
 * assertions.
 *
 * <p>
 * Structural reference: {@code StorageRackDAOTest} (real DAO + real DB +
 * JdbcTemplate-seeded FK parents + {@code @After} cleanup). The base class is
 * {@code @Transactional(propagation = NOT_SUPPORTED)}, so writes are NOT rolled
 * back — {@code @After} cleanup in FK order is mandatory.
 */
public class OrderLabelRequestDAOImplTest extends BaseWebContextSensitiveTest {

    private static final String ACCESSION_A = "OLR-TEST-A";
    private static final String ACCESSION_B = "OLR-TEST-B";
    private static final String PRESET_A_NAME = "OLR Test Preset Per-Sample";
    private static final String PRESET_B_NAME = "OLR Test Preset Per-Order";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OrderLabelRequestDAO orderLabelRequestDAO;

    @Autowired
    private org.openelisglobal.sample.dao.SampleDAO sampleDAO;

    @Autowired
    private org.openelisglobal.sampleitem.dao.SampleItemDAO sampleItemDAO;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    private JdbcTemplate jdbcTemplate;

    private String sampleAId;
    private String sampleBId;
    private String sampleItemAId;
    private String sampleItemBId;
    private Integer presetAId;
    private Integer presetBId;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();

        // --- Legacy FK parents: sample + sample_item (numeric(10,0) PKs).
        // Columns mirror testdata/sample-storage-integration-test-data.xml.
        sampleAId = String.valueOf(jdbcTemplate.queryForObject("SELECT nextval('sample_seq')", Long.class));
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated, sys_user_id) "
                        + "VALUES (?, ?, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)",
                Long.valueOf(sampleAId), ACCESSION_A);

        sampleBId = String.valueOf(jdbcTemplate.queryForObject("SELECT nextval('sample_seq')", Long.class));
        jdbcTemplate.update(
                "INSERT INTO sample (id, accession_number, entered_date, received_date, lastupdated, sys_user_id) "
                        + "VALUES (?, ?, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1)",
                Long.valueOf(sampleBId), ACCESSION_B);

        sampleItemAId = insertSampleItem(sampleAId);
        sampleItemBId = insertSampleItem(sampleBId);

        // --- New-pattern FK parent: LabelPreset persisted via its own real DAO.
        presetAId = labelPresetDAO.insert(newPreset(PRESET_A_NAME, true, false));
        presetBId = labelPresetDAO.insert(newPreset(PRESET_B_NAME, false, true));

        // --- The rows under test. Discriminating layout:
        // r1: sampleA / sampleItemA / presetA (per-sample)
        // r2: sampleA / (no sample item) / presetB (per-order)
        // r3: sampleB / sampleItemB / presetA (per-sample, DIFFERENT parent sample)
        orderLabelRequestDAO.insert(newRequest(sampleAId, sampleItemAId, presetAId, 2));
        orderLabelRequestDAO.insert(newRequest(sampleAId, null, presetBId, 1));
        orderLabelRequestDAO.insert(newRequest(sampleBId, sampleItemBId, presetAId, 3));
    }

    @After
    public void tearDown() {
        cleanTestData();
    }

    /**
     * listByParentSampleId returns every request rooted at the parent sample
     * (per-order AND per-sample rows) and excludes requests rooted at a different
     * parent sample.
     */
    @Test
    public void listByParentSampleId_returnsOnlyRequestsForThatParentSample() {
        List<OrderLabelRequest> forSampleA = orderLabelRequestDAO.listByParentSampleId(sampleAId);

        assertEquals("sampleA should have exactly its 2 requests (per-sample + per-order)", 2, forSampleA.size());
        assertTrue("every returned row must belong to sampleA",
                forSampleA.stream().allMatch(r -> sampleAId.equals(r.getParentSample().getId())));
        // Exclusion: the sampleB request must NOT leak in.
        assertTrue("sampleB's request must be excluded",
                forSampleA.stream().noneMatch(r -> sampleBId.equals(r.getParentSample().getId())));

        List<OrderLabelRequest> forSampleB = orderLabelRequestDAO.listByParentSampleId(sampleBId);
        assertEquals("sampleB should have exactly its 1 request", 1, forSampleB.size());
        assertEquals(sampleBId, forSampleB.get(0).getParentSample().getId());
    }

    /**
     * listBySampleItemId returns only the request bound to that sample item —
     * excluding the per-order row (null sample item) and the request bound to a
     * different sample item.
     */
    @Test
    public void listBySampleItemId_returnsOnlyRequestForThatSampleItem() {
        List<OrderLabelRequest> forItemA = orderLabelRequestDAO.listBySampleItemId(sampleItemAId);

        assertEquals("only the per-sample request for sampleItemA should match", 1, forItemA.size());
        OrderLabelRequest match = forItemA.get(0);
        assertNotNull("matched row must carry its sample item", match.getSampleItem());
        assertEquals(sampleItemAId, match.getSampleItem().getId());
        assertEquals("matched row is the per-sample request (qty 2)", Integer.valueOf(2), match.getQty());

        // Exclusion: the other sample item's request must not appear.
        List<OrderLabelRequest> forItemB = orderLabelRequestDAO.listBySampleItemId(sampleItemBId);
        assertEquals(1, forItemB.size());
        assertEquals(sampleItemBId, forItemB.get(0).getSampleItem().getId());
    }

    /**
     * listByPresetId returns every request referencing the preset across parent
     * samples, and excludes requests referencing a different preset.
     */
    @Test
    public void listByPresetId_returnsOnlyRequestsForThatPreset() {
        List<OrderLabelRequest> forPresetA = orderLabelRequestDAO.listByPresetId(presetAId);

        assertEquals("presetA is referenced by 2 requests (across both parent samples)", 2, forPresetA.size());
        assertTrue("every returned row must reference presetA",
                forPresetA.stream().allMatch(r -> presetAId.equals(r.getPreset().getId())));
        List<String> parentSampleIds = forPresetA.stream().map(r -> r.getParentSample().getId())
                .collect(Collectors.toList());
        assertTrue("presetA spans both parent samples",
                parentSampleIds.contains(sampleAId) && parentSampleIds.contains(sampleBId));

        // Exclusion: presetB is referenced by exactly the one per-order request.
        List<OrderLabelRequest> forPresetB = orderLabelRequestDAO.listByPresetId(presetBId);
        assertEquals(1, forPresetB.size());
        assertEquals(presetBId, forPresetB.get(0).getPreset().getId());
    }

    /**
     * Insert round-trip: a persisted request is re-readable by surrogate PK with
     * its FK associations and qty intact (the precondition the finders rely on).
     */
    @Test
    public void insert_persistsRequestReadableByIdWithFksAndQty() {
        Integer newId = orderLabelRequestDAO.insert(newRequest(sampleAId, sampleItemAId, presetBId, 7));

        OrderLabelRequest reloaded = orderLabelRequestDAO.get(newId).orElse(null);
        assertNotNull("inserted request must be retrievable by id", reloaded);
        assertEquals(sampleAId, reloaded.getParentSample().getId());
        assertEquals(sampleItemAId, reloaded.getSampleItem().getId());
        assertEquals(presetBId, reloaded.getPreset().getId());
        assertEquals(Integer.valueOf(7), reloaded.getQty());
        assertNotNull("preset snapshot must round-trip non-null", reloaded.getPresetSnapshot());
    }

    /**
     * Inserts a {@code sample_item} child of the given sample via JDBC and returns
     * its generated id. Column set ({@code status_id}, {@code typeosamp_id} both
     * seeded to 1, {@code sort_order}) mirrors the known-good
     * {@code testdata/sample-storage-integration-test-data.xml} rows;
     * {@code status_id} is NOT NULL in the legacy schema.
     */
    private String insertSampleItem(String sampleId) {
        String sampleItemId = String
                .valueOf(jdbcTemplate.queryForObject("SELECT nextval('sample_item_seq')", Long.class));
        jdbcTemplate.update(
                "INSERT INTO sample_item (id, samp_id, sort_order, typeosamp_id, status_id, lastupdated) "
                        + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)",
                Long.valueOf(sampleItemId), Long.valueOf(sampleId), 1, 1, 1);
        return sampleItemId;
    }

    private LabelPreset newPreset(String name, boolean printsPerSample, boolean printsPerOrder) {
        LabelPreset preset = new LabelPreset();
        preset.setName(name);
        preset.setHeightMm(25);
        preset.setWidthMm(76);
        preset.setBarcodeType(BarcodeType.CODE_128);
        preset.setPrintsPerSample(printsPerSample);
        preset.setPrintsPerOrder(printsPerOrder);
        preset.setDefaultPerSample(printsPerSample ? 1 : 0);
        preset.setMaxPerSample(10);
        preset.setDefaultPerOrder(printsPerOrder ? 1 : 0);
        preset.setMaxPerOrder(10);
        preset.setIsSystem(false);
        preset.setIsActive(true);
        preset.setSysUserId(TEST_SYS_USER_ID);
        return preset;
    }

    private OrderLabelRequest newRequest(String parentSampleId, String sampleItemId, Integer presetId, int qty) {
        OrderLabelRequest request = new OrderLabelRequest();

        // Load the FK parents as Hibernate-managed (persistent) entities. They were
        // seeded via raw JDBC, so a `new Sample()` stub would be transient and the
        // not-null @ManyToOne would reject it (TransientPropertyValueException).
        Sample parentSample = sampleDAO.get(parentSampleId).orElseThrow();
        request.setParentSample(parentSample);

        if (sampleItemId != null) {
            SampleItem sampleItem = sampleItemDAO.get(sampleItemId).orElseThrow();
            request.setSampleItem(sampleItem);
        }

        LabelPreset preset = labelPresetDAO.get(presetId).orElseThrow();
        request.setPreset(preset);

        request.setQty(qty);
        request.setPresetSnapshot(buildSnapshot(preset, qty));
        request.setSysUserId(TEST_SYS_USER_ID);
        return request;
    }

    /**
     * Minimal valid {@link PresetSnapshotDto} per FRS §7.3.1 — populated only to
     * satisfy the NOT-NULL {@code preset_snapshot} column. Deep JSONB shape
     * fidelity is asserted by {@code PresetSnapshotJsonbRoundtripTest} (T026).
     */
    private PresetSnapshotDto buildSnapshot(LabelPreset preset, int qty) {
        PresetSnapshotDto snapshot = new PresetSnapshotDto();

        PresetSnapshotDto.PresetSnapshotPreset snapPreset = new PresetSnapshotDto.PresetSnapshotPreset();
        snapPreset.setId(preset.getId());
        snapPreset.setName(preset.getName());
        snapPreset.setHeightMm(preset.getHeightMm());
        snapPreset.setWidthMm(preset.getWidthMm());
        snapPreset.setBarcodeType(preset.getBarcodeType().name());
        snapshot.setPreset(snapPreset);

        PresetSnapshotDto.PresetSnapshotField labNumber = new PresetSnapshotDto.PresetSnapshotField();
        labNumber.setFieldKey("LAB_NUMBER");
        labNumber.setFieldLabel("Lab Number");
        labNumber.setIsRequired(true);
        labNumber.setDisplayOrder(1);
        snapshot.getFields().add(labNumber);

        return snapshot;
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM order_label_request WHERE preset_id IN "
                    + "(SELECT id FROM label_preset WHERE name IN ('" + PRESET_A_NAME + "', '" + PRESET_B_NAME + "'))");
            jdbcTemplate.execute(
                    "DELETE FROM order_label_request WHERE parent_sample_id IN " + "(SELECT id FROM sample WHERE "
                            + "accession_number IN ('" + ACCESSION_A + "', '" + ACCESSION_B + "'))");
            jdbcTemplate.execute(
                    "DELETE FROM label_preset WHERE name IN ('" + PRESET_A_NAME + "', '" + PRESET_B_NAME + "')");
            jdbcTemplate.execute(
                    "DELETE FROM sample_item WHERE samp_id IN " + "(SELECT id FROM sample WHERE accession_number IN ('"
                            + ACCESSION_A + "', '" + ACCESSION_B + "'))");
            jdbcTemplate.execute(
                    "DELETE FROM sample WHERE accession_number IN ('" + ACCESSION_A + "', '" + ACCESSION_B + "')");
        } catch (Exception e) {
            // Ignore cleanup errors (e.g. first run before tables have rows).
        }
    }
}
