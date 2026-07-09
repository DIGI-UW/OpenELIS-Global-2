package org.openelisglobal.testresultcomponent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-949 M5 / OGC-937 — ORM validation for {@link TestResultComponent} against
 * a real Testcontainers Postgres (discharges M1 T103 for this entity).
 *
 * BaseWebContextSensitiveTest runs with @Transactional(NOT_SUPPORTED), so the
 * service genuinely COMMITs and reads back across a real round-trip — this is
 * what catches column-name / type-mapping errors (the LIMSStringNumberUserType
 * numeric-id columns, Integer ↔ numeric(10), the lastupdated/last_updated
 * 
 * @Version split). Seeded rows are cleaned up in @After.
 */
public class TestResultComponentServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95001L;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String uomId;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        uomId = jdbc.queryForObject("SELECT min(id) FROM clinlims.unit_of_measure", String.class);
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "ORM Component Test", "component ORM test", UUID.randomUUID().toString());
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        try {
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", TEST_ID);
        } catch (Exception ignored) {
            // table absent before changeset 041 (degenerate red phase)
        }
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void persistsAndReadsBackEveryMappedField() {
        TestResultComponent c = new TestResultComponent();
        c.setTestId(String.valueOf(TEST_ID));
        c.setCode("SYS");
        c.setLabel("Systolic");
        c.setDisplayOrder(1);
        c.setResultType("N");
        c.setUomId(uomId);
        c.setSignificantDigits(2);
        c.setDefaultResult("120");
        c.setAllowMultipleReadings(true);
        c.setIsActive("Y");
        c.setSysUserId("1");

        String id = componentService.insert(c);

        // Real round-trip: read it back from the committed row.
        TestResultComponent loaded = componentService.get(id);
        assertEquals(String.valueOf(TEST_ID), loaded.getTestId());
        assertEquals("SYS", loaded.getCode());
        assertEquals("Systolic", loaded.getLabel());
        assertEquals(Integer.valueOf(1), loaded.getDisplayOrder());
        assertEquals("N", loaded.getResultType());
        assertEquals(uomId, loaded.getUomId());
        assertEquals(Integer.valueOf(2), loaded.getSignificantDigits());
        assertEquals("120", loaded.getDefaultResult());
        assertTrue(loaded.getAllowMultipleReadings());
        assertEquals("Y", loaded.getIsActive());
    }

    @Test
    public void nullableColumnsRoundTripAsNull() {
        TestResultComponent c = new TestResultComponent();
        c.setTestId(String.valueOf(TEST_ID));
        c.setCode("PRIMARY");
        c.setLabel("Primary");
        c.setDisplayOrder(0);
        // resultType, uomId, significantDigits, defaultResult left null
        c.setSysUserId("1");

        TestResultComponent loaded = componentService.get(componentService.insert(c));
        assertNull("uom_id must round-trip as null when unset", loaded.getUomId());
        assertNull(loaded.getSignificantDigits());
        assertNull(loaded.getResultType());
        assertNull(loaded.getDefaultResult());
        // BOOLEAN NOT NULL DEFAULT false
        assertFalse(loaded.getAllowMultipleReadings());
    }

    @Test
    public void getActiveComponentsByTestId_excludesInactive_andOrdersByDisplayOrder() {
        insertComponent("B", "Second", 2, "Y");
        insertComponent("A", "First", 1, "Y");
        insertComponent("Z", "Gone", 3, "N");

        List<TestResultComponent> active = componentService.getActiveComponentsByTestId(String.valueOf(TEST_ID));

        assertEquals(2, active.size());
        // ordered by display_order: A (1) before B (2); inactive Z excluded.
        assertEquals("A", active.get(0).getCode());
        assertEquals("B", active.get(1).getCode());
    }

    @Test
    public void getByTestIdAndCode_findsTheComponent_orNull() {
        insertComponent("DIA", "Diastolic", 2, "Y");

        TestResultComponent found = componentService.getByTestIdAndCode(String.valueOf(TEST_ID), "DIA");
        assertEquals("Diastolic", found.getLabel());

        assertNull(componentService.getByTestIdAndCode(String.valueOf(TEST_ID), "NOPE"));
    }

    private void insertComponent(String code, String label, int order, String active) {
        TestResultComponent c = new TestResultComponent();
        c.setTestId(String.valueOf(TEST_ID));
        c.setCode(code);
        c.setLabel(label);
        c.setDisplayOrder(order);
        c.setIsActive(active);
        c.setSysUserId("1");
        componentService.insert(c);
    }
}
