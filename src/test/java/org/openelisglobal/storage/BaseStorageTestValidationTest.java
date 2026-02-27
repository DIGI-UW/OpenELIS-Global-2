package org.openelisglobal.storage;

import static org.junit.Assert.*;

import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.storage.dao.StorageRoomDAO;
import org.openelisglobal.storage.valueholder.StorageRoom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

/**
 * Validation test to verify storage test fixtures load correctly via DBUnit
 * XML.
 *
 * This test verifies that: 1. Storage hierarchy fixtures exist
 * (rooms/devices/shelves/racks) 2. E2E test data is loaded via DBUnit XML 3.
 * Data validation works correctly
 */
@Rollback
public class BaseStorageTestValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private StorageRoomDAO storageRoomDAO;

    @Autowired
    private DataSource dataSource;

    protected JdbcTemplate jdbcTemplate;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        executeDataSetWithStateManagement("testdata/user-role.xml");
        // Load type_of_sample data (required for sample_item typeosamp_id foreign key)
        executeDataSetWithStateManagement("testdata/typeofsample.xml");
        // Load status_of_sample data (required for sample/sample_item status_id foreign
        // key)
        executeDataSetWithStateManagement("testdata/status-of-sample.xml");
        // Load storage hierarchy + E2E test data via DBUnit
        executeDataSetWithStateManagement("testdata/storage-e2e.xml");
    }

    @Test
    public void testFoundationDataLoaded() {
        StorageRoom mainRoom = storageRoomDAO.findByCode("MAIN");
        assertNotNull("Main Laboratory room should exist (fixture)", mainRoom);
        assertEquals("Main Laboratory", mainRoom.getName());

        StorageRoom secRoom = storageRoomDAO.findByCode("SEC");
        assertNotNull("Secondary Laboratory room should exist (fixture)", secRoom);
        assertEquals("Secondary Laboratory", secRoom.getName());

        StorageRoom inactiveRoom = storageRoomDAO.findByCode("INACTIVE");
        assertNotNull("Inactive Room should exist (fixture)", inactiveRoom);
        assertEquals("Inactive Room", inactiveRoom.getName());
    }

    @Test
    public void testE2EDataLoaded() {
        // Verify E2E test data (from DBUnit XML) exists
        // Note: Using direct ID range check instead of LIKE pattern due to transaction
        // isolation
        Integer patientCount = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM patient WHERE id BETWEEN 1000 AND 1002", Integer.class);
        assertNotNull("E2E patients should exist", patientCount);
        assertTrue("Should have 3 E2E patients, found " + patientCount, patientCount == 3);

        // Check samples by ID range (DBUnit loads IDs 1000-1009)
        Integer sampleCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sample WHERE id BETWEEN 1000 AND 1009",
                Integer.class);
        assertNotNull("E2E samples should exist", sampleCount);
        assertTrue("Should have 10 E2E samples, found " + sampleCount, sampleCount == 10);

        Integer sampleItemCount = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM sample_item WHERE id BETWEEN 10001 AND 10093", Integer.class);
        assertNotNull("E2E sample items should exist", sampleItemCount);
        assertTrue("Should have at least 10 E2E sample items, found " + sampleItemCount, sampleItemCount >= 10);
    }

    @Test
    public void testStorageAssignmentsLoaded() {
        // Verify storage assignments (from DBUnit XML) exist
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Check for E2E fixture assignments (IDs 5000-5013) to avoid conflicts with
        // test-created data (1000+)
        Integer assignmentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sample_storage_assignment WHERE id BETWEEN 5000 AND 5013", Integer.class);
        assertNotNull("Storage assignments should exist", assignmentCount);
        assertTrue("Should have 12 E2E storage assignments, found " + assignmentCount, assignmentCount == 12);
    }
}
