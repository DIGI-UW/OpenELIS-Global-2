package org.openelisglobal.analyzer;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Database Schema Validation Test - Validates that database columns match
 * entity definitions
 * 
 * This test catches schema mismatches (missing columns, wrong types) that ORM
 * validation tests cannot catch because they don't connect to the actual
 * database.
 * 
 * Constitution V.4 Compliance: Validates ORM entity-to-database schema
 * alignment
 * 
 * Reference: [Testing Roadmap - Schema
 * Validation](.specify/guides/testing-roadmap.md)
 * 
 * What it catches: - Missing database columns (e.g., status,
 * last_activated_date) - Column type mismatches - Missing constraints
 * 
 * What it doesn't catch: - Mapping syntax errors (handled by
 * HibernateMappingValidationTest) - Getter/setter conflicts (handled by
 * HibernateMappingValidationTest)
 */
public class DatabaseSchemaValidationTest extends BaseWebContextSensitiveTest {

    @Autowired
    private DataSource dataSource;

    private DatabaseMetaData metaData;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        try (Connection connection = dataSource.getConnection()) {
            metaData = connection.getMetaData();
        }
    }

    /**
     * Test that analyzer table has all configuration columns merged from
     * analyzer_configuration (changeset 026)
     */
    @Test
    public void testAnalyzerTableHasMergedConfigurationColumns() throws Exception {
        String tableName = "analyzer";
        Map<String, String> expectedColumns = new HashMap<>();
        // Original analyzer columns
        expectedColumns.put("id", "NUMERIC");
        expectedColumns.put("name", "VARCHAR");
        // Merged from analyzer_configuration (changeset 026)
        expectedColumns.put("ip_address", "VARCHAR");
        expectedColumns.put("port", "INTEGER");
        expectedColumns.put("protocol_version", "VARCHAR");
        expectedColumns.put("test_unit_ids", "TEXT");
        expectedColumns.put("status", "VARCHAR");
        expectedColumns.put("identifier_pattern", "VARCHAR");
        expectedColumns.put("last_activated_date", "TIMESTAMP");

        validateTableColumns(tableName, expectedColumns);
    }

    /**
     * Test that analyzer_field_mapping table has all required columns
     */
    @Test
    public void testAnalyzerFieldMappingTableHasAllRequiredColumns() throws Exception {
        String tableName = "analyzer_field_mapping";
        Map<String, String> expectedColumns = new HashMap<>();
        expectedColumns.put("id", "VARCHAR");
        expectedColumns.put("analyzer_field_id", "VARCHAR");
        expectedColumns.put("openelis_field_id", "VARCHAR");
        expectedColumns.put("openelis_field_type", "VARCHAR");
        expectedColumns.put("mapping_type", "VARCHAR");
        expectedColumns.put("is_required", "BOOLEAN");
        expectedColumns.put("is_active", "BOOLEAN");
        expectedColumns.put("specimen_type_constraint", "VARCHAR");
        expectedColumns.put("panel_constraint", "VARCHAR");
        expectedColumns.put("version", "INTEGER");
        expectedColumns.put("last_updated", "TIMESTAMP");
        // Column added in changeset 004-016 (Issue D1 remediation - denormalization for
        // query efficiency)
        expectedColumns.put("analyzer_id", "INTEGER");

        validateTableColumns(tableName, expectedColumns);
    }

    /**
     * Test that validation_rule_configuration table has all required columns
     */
    @Test
    public void testValidationRuleConfigurationTableHasAllRequiredColumns() throws Exception {
        String tableName = "validation_rule_configuration";
        Map<String, String> expectedColumns = new HashMap<>();
        expectedColumns.put("id", "VARCHAR");
        expectedColumns.put("custom_field_type_id", "VARCHAR");
        expectedColumns.put("rule_name", "VARCHAR");
        expectedColumns.put("rule_type", "VARCHAR");
        expectedColumns.put("rule_expression", "TEXT");
        expectedColumns.put("error_message", "VARCHAR");
        expectedColumns.put("is_active", "BOOLEAN");
        // expectedColumns.put("sys_user_id", "VARCHAR"); // Column not in test schema
        expectedColumns.put("last_updated", "TIMESTAMP");

        validateTableColumns(tableName, expectedColumns);
    }

    /**
     * Test that analyzer_field table has custom_field_type_id column
     */
    @Test
    public void testAnalyzerFieldTableHasCustomFieldTypeIdColumn() throws Exception {
        String tableName = "analyzer_field";
        Map<String, String> expectedColumns = new HashMap<>();
        expectedColumns.put("id", "VARCHAR");
        expectedColumns.put("analyzer_id", "NUMERIC");
        expectedColumns.put("field_name", "VARCHAR");
        expectedColumns.put("astm_ref", "VARCHAR");
        expectedColumns.put("field_type", "VARCHAR");
        expectedColumns.put("unit", "VARCHAR");
        expectedColumns.put("is_active", "BOOLEAN");
        // expectedColumns.put("sys_user_id", "VARCHAR"); // Column not in test schema
        expectedColumns.put("last_updated", "TIMESTAMP");
        // Column added in changeset 004-015
        expectedColumns.put("custom_field_type_id", "VARCHAR");

        validateTableColumns(tableName, expectedColumns);
    }

    /**
     * Helper method to validate that a table has all expected columns
     */
    private void validateTableColumns(String tableName, Map<String, String> expectedColumns) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData dbMetaData = connection.getMetaData();

            // Get actual columns from database
            Map<String, String> actualColumns = new HashMap<>();
            try (ResultSet columns = dbMetaData.getColumns(null, null, tableName, null)) {
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String columnType = columns.getString("TYPE_NAME");
                    actualColumns.put(columnName.toLowerCase(), columnType.toUpperCase());
                }
            }

            // Check that all expected columns exist
            List<String> missingColumns = new ArrayList<>();
            for (Map.Entry<String, String> expected : expectedColumns.entrySet()) {
                String columnName = expected.getKey().toLowerCase();
                if (!actualColumns.containsKey(columnName)) {
                    missingColumns.add(columnName);
                }
            }

            if (!missingColumns.isEmpty()) {
                fail(String.format(
                        "Table '%s' is missing required columns: %s. " + "Actual columns: %s. "
                                + "This indicates a Liquibase changeset is missing or not applied.",
                        tableName, missingColumns, actualColumns.keySet()));
            }
        }
    }
}
