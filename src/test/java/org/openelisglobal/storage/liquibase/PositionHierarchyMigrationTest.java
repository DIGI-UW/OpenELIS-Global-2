package org.openelisglobal.storage.liquibase;

import static org.junit.Assert.*;

import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Database migration test for Position Hierarchy Structure Update (2-5 Level
 * Support).
 * 
 * T026c: Verifies schema changes from changeset
 * 004-update-position-hierarchy-structure.xml
 * 
 * Tests: - parent_device_id column exists and is NOT NULL - parent_shelf_id
 * column exists and is NULL (nullable) - parent_rack_id column is changed to
 * NULL (nullable) - coordinate column is changed to NULL (nullable) - CHECK
 * constraints are created
 * 
 * Note: This test requires the database migration to have run. It will be run
 * after the application starts and migrations are applied.
 */
public class PositionHierarchyMigrationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    /**
     * Test that parent_device_id column exists and is NOT NULL
     */
    @Test
    public void testParentDeviceIdNotNull() {
        // Given: STORAGE_POSITION table exists
        // When: Querying column information
        String sql = "SELECT " + "    column_name, " + "    data_type, " + "    is_nullable "
                + "FROM information_schema.columns " + "WHERE table_name = 'storage_position' "
                + "    AND column_name = 'parent_device_id'";

        var result = jdbcTemplate.queryForMap(sql);

        // Then: Column should exist and be NOT NULL
        assertNotNull("parent_device_id column should exist", result);
        assertEquals("parent_device_id", result.get("column_name"));
        assertEquals("NO", result.get("is_nullable")); // PostgreSQL uses 'NO' for NOT NULL
    }

    /**
     * Test that parent_shelf_id column exists and is NULL (nullable)
     */
    @Test
    public void testParentShelfIdNullable() {
        // Given: STORAGE_POSITION table exists
        // When: Querying column information
        String sql = "SELECT " + "    column_name, " + "    data_type, " + "    is_nullable "
                + "FROM information_schema.columns " + "WHERE table_name = 'storage_position' "
                + "    AND column_name = 'parent_shelf_id'";

        var result = jdbcTemplate.queryForMap(sql);

        // Then: Column should exist and be nullable
        assertNotNull("parent_shelf_id column should exist", result);
        assertEquals("parent_shelf_id", result.get("column_name"));
        assertEquals("YES", result.get("is_nullable")); // PostgreSQL uses 'YES' for nullable
    }

    /**
     * Test that parent_rack_id column is changed to NULL (nullable)
     */
    @Test
    public void testParentRackIdNullable() {
        // Given: STORAGE_POSITION table exists
        // When: Querying column information
        String sql = "SELECT " + "    column_name, " + "    data_type, " + "    is_nullable "
                + "FROM information_schema.columns " + "WHERE table_name = 'storage_position' "
                + "    AND column_name = 'parent_rack_id'";

        var result = jdbcTemplate.queryForMap(sql);

        // Then: Column should exist and be nullable (changed from NOT NULL)
        assertNotNull("parent_rack_id column should exist", result);
        assertEquals("parent_rack_id", result.get("column_name"));
        assertEquals("YES", result.get("is_nullable")); // Should be nullable after migration
    }

    /**
     * Test that coordinate column is changed to NULL (nullable)
     */
    @Test
    public void testCoordinateNullable() {
        // Given: STORAGE_POSITION table exists
        // When: Querying column information
        String sql = "SELECT " + "    column_name, " + "    data_type, " + "    is_nullable "
                + "FROM information_schema.columns " + "WHERE table_name = 'storage_position' "
                + "    AND column_name = 'coordinate'";

        var result = jdbcTemplate.queryForMap(sql);

        // Then: Column should exist and be nullable (changed from NOT NULL)
        assertNotNull("coordinate column should exist", result);
        assertEquals("coordinate", result.get("column_name"));
        assertEquals("YES", result.get("is_nullable")); // Should be nullable after migration
    }

    /**
     * Test that CHECK constraints are created
     */
    @Test
    public void testCheckConstraints() {
        // Given: STORAGE_POSITION table exists
        // When: Querying constraint information
        String sql = "SELECT " + "    conname as constraint_name, "
                + "    pg_get_constraintdef(oid) as constraint_definition " + "FROM pg_constraint "
                + "WHERE conrelid = 'storage_position'::regclass " + "    AND contype = 'c' " + // 'c' for CHECK
                                                                                                // constraint
                "ORDER BY conname";

        var constraints = jdbcTemplate.queryForList(sql);

        // Then: Should have CHECK constraints
        assertTrue("Should have at least one CHECK constraint", constraints.size() > 0);

        // Verify constraint: If parent_rack_id is NOT NULL, then parent_shelf_id must
        // also be NOT NULL
        boolean foundRackShelfConstraint = constraints.stream()
                .anyMatch(constraint -> constraint.get("constraint_definition").toString().contains("parent_rack_id")
                        && constraint.get("constraint_definition").toString().contains("parent_shelf_id"));

        // Verify constraint: If coordinate is NOT NULL, then parent_rack_id must also
        // be NOT NULL
        boolean foundCoordinateRackConstraint = constraints.stream()
                .anyMatch(constraint -> constraint.get("constraint_definition").toString().contains("coordinate")
                        && constraint.get("constraint_definition").toString().contains("parent_rack_id"));

        assertTrue("Should have constraint: if parent_rack_id NOT NULL then parent_shelf_id NOT NULL",
                foundRackShelfConstraint);
        assertTrue("Should have constraint: if coordinate NOT NULL then parent_rack_id NOT NULL",
                foundCoordinateRackConstraint);
    }

    /**
     * Test that foreign key constraint exists for parent_device_id
     */
    @Test
    public void testParentDeviceIdForeignKey() {
        // Given: STORAGE_POSITION table exists
        // When: Querying foreign key constraints for parent_device_id
        // Use pg_get_constraintdef to check if constraint involves parent_device_id
        // column
        String sql = "SELECT " + "    conname as constraint_name, " + "    confrelid::regclass as referenced_table, "
                + "    pg_get_constraintdef(oid) as constraint_def " + "FROM pg_constraint "
                + "WHERE conrelid = 'storage_position'::regclass " + "    AND contype = 'f' " + // 'f' for FOREIGN KEY
                "    AND pg_get_constraintdef(oid) LIKE '%parent_device_id%'";

        var results = jdbcTemplate.queryForList(sql);

        // Then: Foreign key should exist and reference storage_device
        assertNotNull("parent_device_id foreign key should exist", results);
        assertTrue("Should have at least one foreign key constraint on parent_device_id", results.size() > 0);

        // Find the constraint that references storage_device
        boolean foundDeviceConstraint = results.stream()
                .anyMatch(result -> "storage_device".equals(result.get("referenced_table").toString()));

        assertTrue("Should have foreign key constraint on parent_device_id referencing storage_device",
                foundDeviceConstraint);
    }
}
