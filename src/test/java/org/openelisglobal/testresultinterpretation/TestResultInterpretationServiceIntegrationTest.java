package org.openelisglobal.testresultinterpretation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.openelisglobal.testresultinterpretation.service.TestResultInterpretationService;
import org.openelisglobal.testresultinterpretation.valueholder.TestResultInterpretation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * OGC-949 M5 / OGC-937 — ORM validation for {@link TestResultInterpretation}
 * against a real Testcontainers Postgres (discharges M1 T103 for this entity).
 * Interpretations hang off a parent {@link TestResultComponent} via a UUID
 * component_id FK; the test seeds that parent first.
 */
public class TestResultInterpretationServiceIntegrationTest extends BaseWebContextSensitiveTest {

    private static final long TEST_ID = 95011L;

    @Autowired
    private TestResultInterpretationService interpretationService;

    @Autowired
    private TestResultComponentService componentService;

    @Autowired
    private javax.sql.DataSource dataSource;

    private JdbcTemplate jdbc;
    private String componentId;

    @Before
    public void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        cleanup();
        jdbc.update(
                "INSERT INTO clinlims.test (id, name, description, is_active, guid, lastupdated)"
                        + " VALUES (?, ?, ?, 'Y', ?, NOW())",
                TEST_ID, "ORM Interp Test", "interpretation ORM test", UUID.randomUUID().toString());

        TestResultComponent parent = new TestResultComponent();
        parent.setTestId(String.valueOf(TEST_ID));
        parent.setCode("PRIMARY");
        parent.setLabel("Primary");
        parent.setDisplayOrder(0);
        parent.setSysUserId("1");
        componentId = componentService.insert(parent);
    }

    @After
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        // FK order: interpretation -> component -> test.
        try {
            jdbc.update("DELETE FROM clinlims.test_result_interpretation i USING clinlims.test_result_component c"
                    + " WHERE i.component_id = c.id AND c.test_id = ?", TEST_ID);
        } catch (Exception ignored) {
            // tables absent before changeset 041
        }
        try {
            jdbc.update("DELETE FROM clinlims.test_result_component WHERE test_id = ?", TEST_ID);
        } catch (Exception ignored) {
            // table absent before changeset 041
        }
        jdbc.update("DELETE FROM clinlims.test WHERE id = ?", TEST_ID);
    }

    @Test
    public void persistsAndReadsBackEveryMappedField() {
        TestResultInterpretation i = new TestResultInterpretation();
        i.setComponentId(componentId);
        i.setValueMatch("POS");
        i.setInterpretationText("Detected");
        i.setSeverity("ABNORMAL");
        i.setColor("red");
        i.setDisplayOrder(1);
        i.setIsActive("Y");
        i.setSysUserId("1");

        TestResultInterpretation loaded = interpretationService.get(interpretationService.insert(i));
        assertEquals(componentId, loaded.getComponentId());
        assertEquals("POS", loaded.getValueMatch());
        assertEquals("Detected", loaded.getInterpretationText());
        assertEquals("ABNORMAL", loaded.getSeverity());
        assertEquals("red", loaded.getColor());
        assertEquals(Integer.valueOf(1), loaded.getDisplayOrder());
        assertEquals("Y", loaded.getIsActive());
    }

    @Test
    public void nullableTextColumnsRoundTripAsNull() {
        TestResultInterpretation i = new TestResultInterpretation();
        i.setComponentId(componentId);
        i.setDisplayOrder(0);
        i.setSysUserId("1");

        TestResultInterpretation loaded = interpretationService.get(interpretationService.insert(i));
        assertNull(loaded.getValueMatch());
        assertNull(loaded.getInterpretationText());
        assertNull(loaded.getSeverity());
        assertNull(loaded.getColor());
    }

    @Test
    public void getActiveByComponentId_excludesInactive_andOrdersByDisplayOrder() {
        insertInterpretation("second", 2, "Y");
        insertInterpretation("first", 1, "Y");
        insertInterpretation("gone", 3, "N");

        List<TestResultInterpretation> active = interpretationService.getActiveByComponentId(componentId);

        assertEquals(2, active.size());
        assertEquals("first", active.get(0).getInterpretationText());
        assertEquals("second", active.get(1).getInterpretationText());
    }

    private void insertInterpretation(String text, int order, String active) {
        TestResultInterpretation i = new TestResultInterpretation();
        i.setComponentId(componentId);
        i.setInterpretationText(text);
        i.setDisplayOrder(order);
        i.setIsActive(active);
        i.setSysUserId("1");
        interpretationService.insert(i);
    }
}
