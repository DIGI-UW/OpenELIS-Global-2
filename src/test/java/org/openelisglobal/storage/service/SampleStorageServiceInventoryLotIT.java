package org.openelisglobal.storage.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.storage.dao.SampleStorageAssignmentDAO;
import org.openelisglobal.storage.dao.SampleStorageMovementDAO;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.SampleStorageMovement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * OGC-657 integration test: InventoryLot occupant support in
 * SampleStorageServiceImpl, exercised against a real (Testcontainers) Postgres
 * with the actual Liquibase-migrated schema — including changeset 051
 * (occupant_type/inventory_lot_id columns on sample_storage_assignment and
 * sample_storage_movement).
 */
public class SampleStorageServiceInventoryLotIT extends BaseWebContextSensitiveTest {

    @Autowired
    private SampleStorageService sampleStorageService;

    @Autowired
    private SampleStorageAssignmentDAO sampleStorageAssignmentDAO;

    @Autowired
    private SampleStorageMovementDAO sampleStorageMovementDAO;

    @Autowired
    private DataSource dataSource;

    private static final String LOT_1 = "7000";
    private static final String LOT_2 = "7001";
    private static final String ROOM = "7000";
    private static final String INACTIVE_ROOM = "7001";
    private static final String DEVICE = "7000";
    private static final String DEVICE_WITH_INACTIVE_PARENT = "7001";
    private static final String BOX = "7000";

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/inventory-lot-storage-test-data.xml");
    }

    @After
    public void cleanUp() throws Exception {
        cleanRowsInCurrentConnection(new String[] { "sample_storage_movement", "sample_storage_assignment" });
    }

    @Test
    public void assignInventoryLotWithLocation_persistsAssignmentAtRoomLevel() {
        Map<String, Object> response = sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null,
                "initial receipt", "1");

        assertEquals("OGC657 Test Room", response.get("hierarchicalPath"));
        assertNotNull(response.get("assignmentId"));

        SampleStorageAssignment saved = sampleStorageAssignmentDAO.findByInventoryLotId(7000L);
        assertNotNull("Assignment should be persisted", saved);
        assertEquals(SampleStorageAssignment.OCCUPANT_INVENTORY_LOT, saved.getOccupantType());
        assertEquals(Long.valueOf(7000L), saved.getInventoryLotId());
        assertNull("Sample-item occupant should be null for a lot assignment", saved.getSampleItemId());
        assertEquals("room", saved.getLocationType());
    }

    @Test
    public void assignInventoryLotWithLocation_toDeviceLevel_buildsFullHierarchicalPath() {
        Map<String, Object> response = sampleStorageService.assignInventoryLotWithLocation(LOT_1, DEVICE, "device",
                null, null, "1");

        assertEquals("OGC657 Test Room > OGC657 Test Freezer", response.get("hierarchicalPath"));
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenAlreadyAssigned() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, null, "1");
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, null, "1");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenLocationInactive() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, INACTIVE_ROOM, "room", null, null, "1");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenLotDoesNotExist() {
        sampleStorageService.assignInventoryLotWithLocation("999999", ROOM, "room", null, null, "1");
    }

    @Test
    public void moveInventoryLotWithLocation_updatesAssignmentAndWritesMovementAudit() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, "initial", "1");

        String movementId = sampleStorageService.moveInventoryLotWithLocation(LOT_1, DEVICE, "device", null,
                "freezer relocation", null, "1");

        assertNotNull(movementId);

        SampleStorageAssignment updated = sampleStorageAssignmentDAO.findByInventoryLotId(7000L);
        assertEquals("device", updated.getLocationType());
        assertEquals(Integer.valueOf(7000), updated.getLocationId());

        // One movement for the initial assignment, one for the move.
        List<SampleStorageMovement> movements = sampleStorageMovementDAO.findByInventoryLotId(7000L);
        assertEquals(2, movements.size());
        SampleStorageMovement moveRecord = movements.stream().filter(m -> m.getPreviousLocationType() != null)
                .findFirst().orElseThrow(() -> new AssertionError("Expected a movement with a previous location"));
        assertEquals(SampleStorageAssignment.OCCUPANT_INVENTORY_LOT, moveRecord.getOccupantType());
        assertEquals("room", moveRecord.getPreviousLocationType());
        assertEquals("device", moveRecord.getNewLocationType());
        assertEquals("freezer relocation", moveRecord.getReason());
    }

    @Test
    public void moveInventoryLotWithLocation_createsAssignmentWhenLotWasUnassigned() {
        String movementId = sampleStorageService.moveInventoryLotWithLocation(LOT_1, ROOM, "room", null,
                "first placement", null, "1");

        assertNotNull(movementId);
        assertNotNull(sampleStorageAssignmentDAO.findByInventoryLotId(7000L));

        List<SampleStorageMovement> movements = sampleStorageMovementDAO.findByInventoryLotId(7000L);
        assertEquals(1, movements.size());
        assertNull("No previous location for a first-time placement", movements.get(0).getPreviousLocationType());
    }

    @Test
    public void getInventoryLotLocation_returnsEmptyMapWhenUnassigned() {
        Map<String, Object> location = sampleStorageService.getInventoryLotLocation(LOT_1);

        assertTrue(location.isEmpty());
    }

    @Test
    public void getInventoryLotLocation_returnsHierarchicalPathWhenAssigned() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, null, "1");

        Map<String, Object> location = sampleStorageService.getInventoryLotLocation(LOT_1);

        assertEquals("OGC657 Test Room", location.get("hierarchicalPath"));
        assertEquals(LOT_1, location.get("inventoryLotId"));
    }

    @Test
    public void getLocationsForInventoryLots_bulkLookupReturnsAllAssignedLots() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, null, "1");
        sampleStorageService.assignInventoryLotWithLocation(LOT_2, DEVICE, "device", null, null, "1");

        Map<String, Map<String, Object>> locations = sampleStorageService
                .getLocationsForInventoryLots(Arrays.asList(7000L, 7001L));

        assertEquals(2, locations.size());
        assertEquals("OGC657 Test Room", locations.get("7000").get("hierarchicalPath"));
        assertEquals("OGC657 Test Room > OGC657 Test Freezer", locations.get("7001").get("hierarchicalPath"));
    }

    @Test
    public void getLocationsForInventoryLots_omitsUnassignedLots() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, null, "1");

        Map<String, Map<String, Object>> locations = sampleStorageService
                .getLocationsForInventoryLots(Arrays.asList(7000L, 7001L));

        assertEquals(1, locations.size());
        assertFalse("Unassigned lot should not appear in the bulk result", locations.containsKey("7001"));
    }

    // ==========================================================================
    // Validation / error paths
    // ==========================================================================

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenLocationIdBlank() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, "", "room", null, null, "1");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenLocationTypeBlank() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "", null, null, "1");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenLocationTypeInvalid() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "warehouse", null, null, "1");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenLocationIdDoesNotExist() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, "999999", "room", null, null, "1");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_throwsWhenParentRoomInactive() {
        // Device itself is active, but its parent room is inactive — the whole
        // chain must be active, not just the target level.
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, DEVICE_WITH_INACTIVE_PARENT, "device", null, null,
                "1");
    }

    @Test(expected = LIMSRuntimeException.class)
    public void moveInventoryLotWithLocation_throwsWhenTargetLocationInactive() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, null, "1");

        sampleStorageService.moveInventoryLotWithLocation(LOT_1, DEVICE_WITH_INACTIVE_PARENT, "device", null,
                "bad move", null, "1");
    }

    // ==========================================================================
    // Physical-space collision (box + coordinate) — must be enforced regardless
    // of occupant type, since it models a real physical slot.
    // ==========================================================================

    @Test
    public void assignInventoryLotWithLocation_toBoxLevel_recordsPositionCoordinate() {
        Map<String, Object> response = sampleStorageService.assignInventoryLotWithLocation(LOT_1, BOX, "box", "A1",
                null, "1");

        assertEquals("OGC657 Test Room > OGC657 Test Freezer > OGC657 Test Shelf > OGC657 Test Rack > A1",
                response.get("hierarchicalPath"));

        SampleStorageAssignment saved = sampleStorageAssignmentDAO.findByInventoryLotId(7000L);
        assertEquals("A1", saved.getPositionCoordinate());
    }

    @Test(expected = LIMSRuntimeException.class)
    public void assignInventoryLotWithLocation_toBoxLevel_throwsWhenCoordinateAlreadyOccupied() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, BOX, "box", "A1", null, "1");

        sampleStorageService.assignInventoryLotWithLocation(LOT_2, BOX, "box", "A1", null, "1");
    }

    @Test
    public void assignInventoryLotWithLocation_toBoxLevel_differentCoordinatesBothSucceed() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, BOX, "box", "A1", null, "1");
        sampleStorageService.assignInventoryLotWithLocation(LOT_2, BOX, "box", "A2", null, "1");

        assertEquals("A1", sampleStorageAssignmentDAO.findByInventoryLotId(7000L).getPositionCoordinate());
        assertEquals("A2", sampleStorageAssignmentDAO.findByInventoryLotId(7001L).getPositionCoordinate());
    }

    // ==========================================================================
    // Multi-occupant sharing at coarse (non-box) levels is allowed — only a
    // box+coordinate models a unique physical slot.
    // ==========================================================================

    @Test
    public void assignInventoryLotWithLocation_multipleLotsCanShareARoomSimultaneously() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, null, "1");
        sampleStorageService.assignInventoryLotWithLocation(LOT_2, ROOM, "room", null, null, "1");

        assertEquals("room", sampleStorageAssignmentDAO.findByInventoryLotId(7000L).getLocationType());
        assertEquals("room", sampleStorageAssignmentDAO.findByInventoryLotId(7001L).getLocationType());
    }

    // ==========================================================================
    // Multi-move audit chain
    // ==========================================================================

    @Test
    public void moveInventoryLotWithLocation_threeSequentialMoves_buildAConsistentAuditChain() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, ROOM, "room", null, "assigned", "1");
        sampleStorageService.moveInventoryLotWithLocation(LOT_1, DEVICE, "device", null, "move 1", null, "1");
        sampleStorageService.moveInventoryLotWithLocation(LOT_1, ROOM, "room", null, "move 2 (back to room)", null,
                "1");
        sampleStorageService.moveInventoryLotWithLocation(LOT_1, BOX, "box", "B9", "move 3", null, "1");

        // 1 initial assignment + 3 moves = 4 audit rows.
        List<SampleStorageMovement> movements = sampleStorageMovementDAO.findByInventoryLotId(7000L);
        assertEquals(4, movements.size());
        for (SampleStorageMovement m : movements) {
            assertEquals(SampleStorageAssignment.OCCUPANT_INVENTORY_LOT, m.getOccupantType());
            assertEquals(Long.valueOf(7000L), m.getInventoryLotId());
        }

        // Final resting place should be the box with the last move's coordinate.
        SampleStorageAssignment finalAssignment = sampleStorageAssignmentDAO.findByInventoryLotId(7000L);
        assertEquals("box", finalAssignment.getLocationType());
        assertEquals("B9", finalAssignment.getPositionCoordinate());
    }

    // ==========================================================================
    // Regression: the pre-existing SampleItem occupant flow must be unaffected
    // by the occupant_type/inventory_lot_id generalization. Loads the original
    // sample-storage fixture (different IDs, no collision with the 7000s above)
    // to exercise assignSampleItemWithLocation/moveSampleItemWithLocation for
    // real against the migrated schema.
    // ==========================================================================

    @Test
    public void sampleItemOccupant_assignAndMove_stillWorkAfterOccupantGeneralization() throws Exception {
        executeDataSetWithStateManagement("testdata/sample-storage-integration-test-data.xml");
        try {
            // sample_item id 1002 (from that fixture) has no pre-existing assignment.
            Map<String, Object> response = sampleStorageService.assignSampleItemWithLocation("1002", "1000", "room",
                    null, "regression check");
            assertNotNull(response.get("assignmentId"));
            assertEquals("Test Integration Room", response.get("hierarchicalPath"));

            SampleStorageAssignment saved = sampleStorageAssignmentDAO.findBySampleItemId("1002");
            assertEquals(SampleStorageAssignment.OCCUPANT_SAMPLE_ITEM, saved.getOccupantType());
            assertNull("Inventory-lot occupant should be null for a sample assignment", saved.getInventoryLotId());

            String movementId = sampleStorageService.moveSampleItemWithLocation("1002", "1000", "device", null,
                    "regression move", null);
            assertNotNull(movementId);
        } finally {
            // Restore the class fixture so subsequent tests in this class (if any
            // ran after this one) aren't affected by this ad-hoc fixture swap.
            executeDataSetWithStateManagement("testdata/inventory-lot-storage-test-data.xml");
        }
    }

    // ==========================================================================
    // The occupant-exclusivity CHECK constraint must be enforced by the
    // database itself, not just by application code — proven by trying to
    // violate it directly over JDBC.
    // ==========================================================================

    @Test
    public void databaseRejectsAssignmentRowWithBothOccupantIdsSet() throws SQLException {
        String sql = "INSERT INTO clinlims.sample_storage_assignment "
                + "(id, occupant_type, sample_item_id, inventory_lot_id, location_id, location_type, "
                + "assigned_by_user_id, assigned_date) "
                + "VALUES (nextval('sample_storage_assignment_seq'), 'INVENTORY_LOT', 1, 7000, 7000, 'room', 1, ?)";
        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
            fail("Expected the chk_assignment_occupant_exclusive constraint to reject a row with both occupant ids set");
        } catch (SQLException e) {
            assertTrue("Expected a check-constraint violation, got: " + e.getMessage(),
                    e.getMessage().contains("chk_assignment_occupant_exclusive"));
        }
    }

    @Test
    public void releaseInventoryLotLocation_freesTheSlotAndKeepsTheAssignmentForAudit() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, BOX, "box", "A1", "initial", "1");
        assertEquals("Box should read as occupied before release", 1,
                sampleStorageAssignmentDAO.countByLocationTypeAndId("box", Integer.valueOf(BOX)));

        Map<String, Object> result = sampleStorageService.releaseInventoryLotLocation(LOT_1, "Disposal: expired", "1");

        // The slot is free again: occupancy filters on locationType/locationId.
        assertEquals("Box should be empty after release", 0,
                sampleStorageAssignmentDAO.countByLocationTypeAndId("box", Integer.valueOf(BOX)));

        // The row survives for the audit trail, with its location cleared.
        SampleStorageAssignment assignment = sampleStorageAssignmentDAO.findByInventoryLotId(7000L);
        assertNotNull("Assignment row should be kept for audit", assignment);
        assertNull(assignment.getLocationId());
        assertNull(assignment.getLocationType());
        assertNull(assignment.getPositionCoordinate());

        assertNotNull("Should report where the lot used to be", result.get("previousLocation"));

        SampleStorageMovement release = sampleStorageMovementDAO.findByInventoryLotId(7000L).stream()
                .filter(m -> m.getNewLocationType() == null).findFirst()
                .orElseThrow(() -> new AssertionError("Expected a release movement"));
        assertEquals(SampleStorageAssignment.OCCUPANT_INVENTORY_LOT, release.getOccupantType());
        assertEquals("box", release.getPreviousLocationType());
        assertEquals("Disposal: expired", release.getReason());
    }

    @Test
    public void releaseInventoryLotLocation_isANoOpForAnUnassignedLot() {
        Map<String, Object> result = sampleStorageService.releaseInventoryLotLocation(LOT_2, "Disposal", "1");

        assertTrue("Nothing to release should return an empty result", result.isEmpty());
        assertTrue("No movement should be written", sampleStorageMovementDAO.findByInventoryLotId(7001L).isEmpty());
    }

    @Test
    public void updateInventoryLotAssignmentMetadata_editsPositionAndNotesWithoutMoving() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, BOX, "box", "A1", "initial", "1");

        Map<String, Object> result = sampleStorageService.updateInventoryLotAssignmentMetadata(LOT_1, "B2",
                "shifted within the box");

        assertEquals("B2", result.get("positionCoordinate"));
        assertEquals("shifted within the box", result.get("notes"));

        SampleStorageAssignment assignment = sampleStorageAssignmentDAO.findByInventoryLotId(7000L);
        assertEquals("B2", assignment.getPositionCoordinate());
        assertEquals("Container must not change", "box", assignment.getLocationType());
        assertEquals(Integer.valueOf(BOX), assignment.getLocationId());

        // Editing in place is not a move, so it writes no movement row.
        assertEquals("Only the initial assignment movement", 1,
                sampleStorageMovementDAO.findByInventoryLotId(7000L).size());
    }

    @Test
    public void updateInventoryLotAssignmentMetadata_blankClearsAndNullLeavesAlone() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, BOX, "box", "A1", "keep me", "1");

        sampleStorageService.updateInventoryLotAssignmentMetadata(LOT_1, "", null);

        SampleStorageAssignment assignment = sampleStorageAssignmentDAO.findByInventoryLotId(7000L);
        assertNull("Blank coordinate should clear it", assignment.getPositionCoordinate());
        assertEquals("Null notes should leave the existing value", "keep me", assignment.getNotes());
    }

    @Test
    public void updateInventoryLotAssignmentMetadata_throwsWhenLotHasNoAssignment() {
        try {
            sampleStorageService.updateInventoryLotAssignmentMetadata(LOT_2, "A1", null);
            fail("Expected LIMSRuntimeException for a lot with no assignment");
        } catch (LIMSRuntimeException expected) {
            assertTrue(expected.getMessage().contains("No storage assignment"));
        }
    }

    @Test
    public void getAllInventoryLotsWithAssignments_listsLotsWithTheirResolvedLocation() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, BOX, "box", "A1", "initial", "1");

        List<Map<String, Object>> lots = sampleStorageService.getAllInventoryLotsWithAssignments();

        Map<String, Object> row = lots.stream().filter(m -> Long.valueOf(7000L).equals(m.get("id"))).findFirst()
                .orElseThrow(() -> new AssertionError("Expected lot 7000 in the listing"));
        assertNotNull(row.get("lotNumber"));
        assertEquals("A1", row.get("positionCoordinate"));
        assertFalse("Location should resolve to a path", row.get("location").toString().isEmpty());
    }

    @Test
    public void getAllInventoryLotsWithAssignments_reportsAReleasedLotAsUnassigned() {
        sampleStorageService.assignInventoryLotWithLocation(LOT_1, BOX, "box", "A1", "initial", "1");
        sampleStorageService.releaseInventoryLotLocation(LOT_1, "Disposal", "1");

        List<Map<String, Object>> lots = sampleStorageService.getAllInventoryLotsWithAssignments();

        Map<String, Object> row = lots.stream().filter(m -> Long.valueOf(7000L).equals(m.get("id"))).findFirst()
                .orElseThrow(() -> new AssertionError("Released lot should still be listed"));
        assertEquals("Released lot has no location", "", row.get("location"));
    }
}
