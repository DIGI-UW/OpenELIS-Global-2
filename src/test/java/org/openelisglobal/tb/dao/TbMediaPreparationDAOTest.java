package org.openelisglobal.tb.dao;

import static org.junit.Assert.*;

import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.tb.valueholder.TbEnums.MediaQcStatus;
import org.openelisglobal.tb.valueholder.TbEnums.MediaType;
import org.openelisglobal.tb.valueholder.TbMediaPreparation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DAO integration tests for TbMediaPreparationDAO.
 *
 * Tests HQL queries with enum parameters to ensure proper type handling with
 * PostgreSQL. Catches the "character varying = bytea" error that occurs when
 * Hibernate 6 incorrectly serializes enum parameters.
 *
 * Uses BaseWebContextSensitiveTest (legacy pattern) since project doesn't use
 * Spring Boot.
 */
public class TbMediaPreparationDAOTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TbMediaPreparationDAO tbMediaPreparationDAO;

    private JdbcTemplate jdbcTemplate;

    // Test data IDs
    private static final int ID_LJ_PASSED = 9001;
    private static final int ID_MGIT_PENDING = 9002;
    private static final int ID_LJ_FAILED = 9003;
    private static final int ID_MGIT_PASSED_EXPIRED = 9004;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
        cleanTestData();
        insertTestData();
    }

    @After
    public void tearDown() throws Exception {
        cleanTestData();
    }

    private void insertTestData() {
        // Reset sequence to avoid conflicts
        jdbcTemplate.execute("SELECT setval('tb_media_preparation_seq', 10000, false)");

        // LJ batch, PASSED status, not expired (available for inoculation)
        jdbcTemplate.update(
                "INSERT INTO tb_media_preparation (id, batch_id, media_type, qc_status, preparation_date, expiry_date, last_updated) "
                        + "VALUES (?, ?, ?, ?, '2025-01-01', '2025-12-31', CURRENT_TIMESTAMP)",
                ID_LJ_PASSED, "LJ-TEST-001", "LJ", "PASSED");

        // MGIT batch, PENDING status
        jdbcTemplate.update(
                "INSERT INTO tb_media_preparation (id, batch_id, media_type, qc_status, preparation_date, expiry_date, last_updated) "
                        + "VALUES (?, ?, ?, ?, '2025-01-01', '2025-12-31', CURRENT_TIMESTAMP)",
                ID_MGIT_PENDING, "MGIT-TEST-001", "MGIT", "PENDING");

        // LJ batch, FAILED status
        jdbcTemplate.update(
                "INSERT INTO tb_media_preparation (id, batch_id, media_type, qc_status, preparation_date, expiry_date, last_updated) "
                        + "VALUES (?, ?, ?, ?, '2025-01-01', '2025-12-31', CURRENT_TIMESTAMP)",
                ID_LJ_FAILED, "LJ-TEST-002", "LJ", "FAILED");

        // MGIT batch, PASSED but expired (NOT available for inoculation)
        jdbcTemplate.update(
                "INSERT INTO tb_media_preparation (id, batch_id, media_type, qc_status, preparation_date, expiry_date, last_updated) "
                        + "VALUES (?, ?, ?, ?, '2024-01-01', '2024-06-01', CURRENT_TIMESTAMP)",
                ID_MGIT_PASSED_EXPIRED, "MGIT-TEST-002", "MGIT", "PASSED");
    }

    private void cleanTestData() {
        try {
            jdbcTemplate.execute("DELETE FROM tb_media_preparation WHERE id IN (9001, 9002, 9003, 9004)");
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    // ==================== findByMediaType Tests ====================

    /**
     * Test: findByMediaType with LJ enum. This test will fail with "bytea" error if
     * enum handling is broken.
     */
    @Test
    public void testFindByMediaType_LJ_ReturnsTwoResults() {
        // Act - Execute HQL query with enum parameter
        List<TbMediaPreparation> results = tbMediaPreparationDAO.findByMediaType(MediaType.LJ);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 2 LJ batches", 2, results.size());
        assertTrue("All results should be LJ type", results.stream().allMatch(mp -> mp.getMediaType() == MediaType.LJ));
    }

    /**
     * Test: findByMediaType with MGIT enum.
     */
    @Test
    public void testFindByMediaType_MGIT_ReturnsTwoResults() {
        // Act
        List<TbMediaPreparation> results = tbMediaPreparationDAO.findByMediaType(MediaType.MGIT);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 2 MGIT batches", 2, results.size());
        assertTrue("All results should be MGIT type",
                results.stream().allMatch(mp -> mp.getMediaType() == MediaType.MGIT));
    }

    // ==================== findByQcStatus Tests ====================

    /**
     * Test: findByQcStatus with PENDING status. This test will fail with "bytea"
     * error if enum handling is broken.
     */
    @Test
    public void testFindByQcStatus_PENDING_ReturnsOneResult() {
        // Act
        List<TbMediaPreparation> results = tbMediaPreparationDAO.findByQcStatus(MediaQcStatus.PENDING);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 1 PENDING batch", 1, results.size());
        assertEquals("Should be MGIT-TEST-001", "MGIT-TEST-001", results.get(0).getBatchId());
    }

    /**
     * Test: findByQcStatus with PASSED status.
     */
    @Test
    public void testFindByQcStatus_PASSED_ReturnsTwoResults() {
        // Act
        List<TbMediaPreparation> results = tbMediaPreparationDAO.findByQcStatus(MediaQcStatus.PASSED);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 2 PASSED batches", 2, results.size());
        assertTrue("All results should be PASSED status",
                results.stream().allMatch(mp -> mp.getQcStatus() == MediaQcStatus.PASSED));
    }

    /**
     * Test: findByQcStatus with FAILED status.
     */
    @Test
    public void testFindByQcStatus_FAILED_ReturnsOneResult() {
        // Act
        List<TbMediaPreparation> results = tbMediaPreparationDAO.findByQcStatus(MediaQcStatus.FAILED);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 1 FAILED batch", 1, results.size());
        assertEquals("Should be LJ-TEST-002", "LJ-TEST-002", results.get(0).getBatchId());
    }

    // ==================== findAvailableForInoculation Tests ====================

    /**
     * Test: findAvailableForInoculation (PASSED + not expired). This test will fail
     * with "bytea" error if enum handling is broken.
     */
    @Test
    public void testFindAvailableForInoculation_ReturnsOnlyNonExpiredPassed() {
        // Act
        List<TbMediaPreparation> results = tbMediaPreparationDAO.findAvailableForInoculation();

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 1 available batch (LJ-TEST-001)", 1, results.size());
        assertEquals("Should be LJ-TEST-001", "LJ-TEST-001", results.get(0).getBatchId());
        assertEquals("Should be PASSED", MediaQcStatus.PASSED, results.get(0).getQcStatus());
    }

    // ==================== findAvailableByMediaType Tests ====================

    /**
     * Test: findAvailableByMediaType with LJ type. This test will fail with "bytea"
     * error if enum handling is broken.
     */
    @Test
    public void testFindAvailableByMediaType_LJ_ReturnsOneResult() {
        // Act
        List<TbMediaPreparation> results = tbMediaPreparationDAO.findAvailableByMediaType(MediaType.LJ);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 1 available LJ batch", 1, results.size());
        assertEquals("Should be LJ-TEST-001", "LJ-TEST-001", results.get(0).getBatchId());
    }

    /**
     * Test: findAvailableByMediaType with MGIT type. MGIT-TEST-002 is PASSED but
     * expired, so should return 0.
     */
    @Test
    public void testFindAvailableByMediaType_MGIT_ReturnsZeroResults() {
        // Act
        List<TbMediaPreparation> results = tbMediaPreparationDAO.findAvailableByMediaType(MediaType.MGIT);

        // Assert
        assertNotNull("Results should not be null", results);
        assertEquals("Should return 0 available MGIT batches (only one is passed but expired)", 0, results.size());
    }

    // ==================== countByQcStatus Tests ====================

    /**
     * Test: countByQcStatus with PENDING status. This test will fail with "bytea"
     * error if enum handling is broken.
     */
    @Test
    public void testCountByQcStatus_PENDING_ReturnsOne() {
        // Act
        Long count = tbMediaPreparationDAO.countByQcStatus(MediaQcStatus.PENDING);

        // Assert
        assertNotNull("Count should not be null", count);
        assertEquals("Should count 1 PENDING batch", Long.valueOf(1), count);
    }

    /**
     * Test: countByQcStatus with PASSED status.
     */
    @Test
    public void testCountByQcStatus_PASSED_ReturnsTwo() {
        // Act
        Long count = tbMediaPreparationDAO.countByQcStatus(MediaQcStatus.PASSED);

        // Assert
        assertNotNull("Count should not be null", count);
        assertEquals("Should count 2 PASSED batches", Long.valueOf(2), count);
    }

    /**
     * Test: countByQcStatus with FAILED status.
     */
    @Test
    public void testCountByQcStatus_FAILED_ReturnsOne() {
        // Act
        Long count = tbMediaPreparationDAO.countByQcStatus(MediaQcStatus.FAILED);

        // Assert
        assertNotNull("Count should not be null", count);
        assertEquals("Should count 1 FAILED batch", Long.valueOf(1), count);
    }
}
