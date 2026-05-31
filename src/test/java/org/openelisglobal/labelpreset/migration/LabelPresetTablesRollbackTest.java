package org.openelisglobal.labelpreset.migration;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * Schema validation for OGC-285 M2 changeset 029-label-preset-tables.xml (task
 * T022).
 *
 * <p>
 * Mirrors {@link org.openelisglobal.analyzer.DatabaseSchemaValidationTest}: it
 * extends {@link BaseWebContextSensitiveTest}, reads the REAL test
 * (Testcontainers/PostgreSQL) schema that the Spring context materialized by
 * running {@code base-changelog.xml} (which includes {@code 3.3.x.x/base.xml},
 * which includes {@code 029-label-preset-tables.xml}) under the {@code test}
 * Liquibase context, and asserts schema state via
 * {@link java.sql.DatabaseMetaData} / {@code information_schema}. No DAO,
 * entity, or {@code JsonBinaryType} is mocked — the assertions are made against
 * materialized DB state.
 *
 * <p>
 * Why not a true Liquibase rollback (the file name notwithstanding): the Spring
 * context runs Liquibase {@code update} exactly once at init against a single,
 * shared Testcontainers database. Executing a mid-suite {@code rollback} here
 * would drop the {@code label_preset*} tables out from under every sibling DAO
 * test sharing that database. So this test asserts the FORWARD outcome of
 * changeset 029 — all five tables exist with their key constraints — as the
 * feasible proxy for "rollback removes them cleanly," matching the limitation
 * of the OGC-284 schema-validation precedent. As a lightweight guard that the
 * rollback path was actually authored, the final test also asserts each of the
 * five {@code <changeSet>} blocks in 029 declares a {@code <rollback>} element
 * (Constitution Principle VI).
 *
 * <p>
 * Inversion-worthiness: if changeset 029 were broken (a table not created, a
 * named CHECK/UNIQUE/FK constraint dropped, or the changeset never wired into
 * {@code base.xml}), the corresponding assertion here fails against the live
 * schema. These are not source-string checks — they read what PostgreSQL
 * actually built.
 */
public class LabelPresetTablesRollbackTest extends BaseWebContextSensitiveTest {

    private static final String SCHEMA = "clinlims";

    /** All five tables changeset 029 creates (data-model.md §2). */
    private static final List<String> EXPECTED_TABLES = Arrays.asList("label_preset", "label_preset_field",
            "test_label_config", "test_label_preset_link", "order_label_request");

    @Autowired
    private DataSource dataSource;

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Each of the five tables created by changeset 029 must exist in the live
     * schema with a PRIMARY KEY. If the changeset failed to apply (or was never
     * included in base.xml), the table is absent and this fails.
     */
    @Test
    public void changeset029CreatedAllFiveTablesWithPrimaryKeys() throws Exception {
        for (String tableName : EXPECTED_TABLES) {
            assertTableExists(tableName);
            assertTrue("Table '" + tableName + "' must declare a PRIMARY KEY constraint (changeset 029).",
                    hasConstraintOfType(tableName, "PRIMARY KEY"));
        }
    }

    /**
     * Spot-check representative columns on each table so a table that exists but is
     * structurally wrong (missing a column the entity mapping reads) is caught,
     * mirroring DatabaseSchemaValidationTest's column-existence approach.
     */
    @Test
    public void changeset029TablesHaveKeyColumns() throws Exception {
        assertColumnsExist("label_preset", "id", "name", "height_mm", "width_mm", "barcode_type", "prints_per_order",
                "prints_per_sample", "default_per_order", "max_per_order", "default_per_sample", "max_per_sample",
                "is_system", "is_active");
        assertColumnsExist("label_preset_field", "id", "preset_id", "field_key", "source_type", "is_required",
                "display_order");
        assertColumnsExist("test_label_config", "id", "test_id", "allow_order_entry_override");
        assertColumnsExist("test_label_preset_link", "id", "test_id", "preset_id", "default_qty", "max_qty",
                "allow_override");
        assertColumnsExist("order_label_request", "id", "parent_sample_id", "sample_item_id", "preset_id", "qty",
                "preset_snapshot");
    }

    /**
     * label_preset: the named UNIQUE on name plus all six named cross-column CHECK
     * constraints (data-model.md §2.1). The barcode_type / range / scope CHECKs are
     * the defense-in-depth rules from FRS §7.1; dropping any one must fail.
     */
    @Test
    public void labelPresetHasNamedUniqueAndCheckConstraints() throws Exception {
        assertConstraintsExistByName("label_preset_name_uniq", "label_preset_height_range", "label_preset_width_range",
                "label_preset_barcode_type", "label_preset_default_nonneg", "label_preset_max_gte_default",
                "label_preset_scope_required");
    }

    /**
     * label_preset_field: FK to label_preset, both named UNIQUE constraints, and
     * the SYSTEM-only source_type CHECK (data-model.md §2.2).
     */
    @Test
    public void labelPresetFieldHasNamedForeignKeyUniqueAndCheckConstraints() throws Exception {
        assertConstraintsExistByName("fk_label_preset_field_preset", "label_preset_field_order_uniq",
                "label_preset_field_key_uniq", "label_preset_field_source_type");
    }

    /** test_label_config: named FK to test (data-model.md §2.3). */
    @Test
    public void testLabelConfigHasNamedForeignKey() throws Exception {
        assertConstraintsExistByName("fk_test_label_config_test");
    }

    /**
     * test_label_preset_link: both named FKs, the named UNIQUE on (test_id,
     * preset_id), and both named qty CHECK constraints (data-model.md §2.4 / §3.1).
     */
    @Test
    public void testLabelPresetLinkHasNamedForeignKeysUniqueAndCheckConstraints() throws Exception {
        assertConstraintsExistByName("fk_test_label_preset_link_test", "fk_test_label_preset_link_preset",
                "test_label_preset_link_uniq", "test_label_preset_link_qty_nonneg",
                "test_label_preset_link_max_gte_default");
    }

    /**
     * order_label_request: all three named FKs (parent sample, optional sample
     * item, preset) and the named qty CHECK (data-model.md §2.5).
     */
    @Test
    public void orderLabelRequestHasNamedForeignKeysAndCheckConstraint() throws Exception {
        assertConstraintsExistByName("fk_order_label_request_parent_sample", "fk_order_label_request_sample_item",
                "fk_order_label_request_preset", "order_label_request_qty_nonneg");
    }

    /**
     * Lightweight authored-rollback guard: changeset 029 must declare a
     * {@code <rollback>} block for each of its five changesets (Principle VI). This
     * is the one assertion read from the changeset source rather than the live
     * schema, because the harness cannot execute a rollback (see class javadoc).
     */
    @Test
    public void changeset029DeclaresRollbackForEveryChangeSet() throws Exception {
        String changesetXml = readClassPathFile("liquibase/3.3.x.x/029-label-preset-tables.xml");
        int changeSetCount = countOccurrences(changesetXml, "<changeSet ");
        int rollbackCount = countOccurrences(changesetXml, "<rollback>");
        assertTrue("029-label-preset-tables.xml should declare five <changeSet> blocks but found " + changeSetCount,
                changeSetCount == 5);
        assertTrue("029-label-preset-tables.xml should declare a <rollback> for each of its five changesets but found "
                + rollbackCount, rollbackCount == changeSetCount);
    }

    // ---- helpers (mirror DatabaseSchemaValidationTest: per-call connection,
    // case-insensitive name handling) ----

    private void assertTableExists(String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection();
                ResultSet tables = connection.getMetaData().getTables(null, SCHEMA, tableName,
                        new String[] { "TABLE" })) {
            if (!tables.next()) {
                fail("Table '" + tableName + "' was not created in schema '" + SCHEMA
                        + "'. This indicates changeset 029-label-preset-tables.xml is missing,"
                        + " not applied, or not included in base.xml.");
            }
        }
    }

    private void assertColumnsExist(String tableName, String... expectedColumns) throws Exception {
        Set<String> actual = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
                ResultSet columns = connection.getMetaData().getColumns(null, SCHEMA, tableName, null)) {
            while (columns.next()) {
                actual.add(columns.getString("COLUMN_NAME").toLowerCase());
            }
        }
        List<String> missing = new ArrayList<>();
        for (String column : expectedColumns) {
            if (!actual.contains(column.toLowerCase())) {
                missing.add(column);
            }
        }
        if (!missing.isEmpty()) {
            fail("Table '" + tableName + "' is missing required columns: " + missing + ". Actual columns: " + actual
                    + ". This indicates changeset 029-label-preset-tables.xml is missing or not applied.");
        }
    }

    private void assertConstraintsExistByName(String... expectedConstraintNames) throws Exception {
        Set<String> actual = loadConstraintNames();
        List<String> missing = new ArrayList<>();
        for (String name : expectedConstraintNames) {
            if (!actual.contains(name.toLowerCase())) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            fail("Schema '" + SCHEMA + "' is missing expected constraints by name: " + missing
                    + ". This indicates a constraint defined in changeset 029-label-preset-tables.xml"
                    + " was not created. Present constraint names: " + actual);
        }
    }

    /**
     * Load every constraint name in the schema (lowercased) from
     * information_schema. Named PK/UNIQUE/FK/CHECK constraints from changeset 029
     * all surface here; auto-named constraints (e.g. the unnamed UNIQUE on
     * test_label_config.test_id) are deliberately not asserted by name.
     */
    private Set<String> loadConstraintNames() throws Exception {
        Set<String> names = new LinkedHashSet<>();
        String sql = "SELECT constraint_name FROM information_schema.table_constraints WHERE table_schema = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SCHEMA);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString("constraint_name").toLowerCase());
                }
            }
        }
        return names;
    }

    private boolean hasConstraintOfType(String tableName, String constraintType) throws Exception {
        String sql = "SELECT 1 FROM information_schema.table_constraints"
                + " WHERE table_schema = ? AND table_name = ? AND constraint_type = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, SCHEMA);
            statement.setString(2, tableName);
            statement.setString(3, constraintType);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String readClassPathFile(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int index = 0;
        while ((index = haystack.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
