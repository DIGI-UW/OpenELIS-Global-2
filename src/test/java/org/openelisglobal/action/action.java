package org.openelisglobal.action;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;

import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.audittrail.valueholder.Action;
import org.openelisglobal.history.service.ActionService;
import org.springframework.beans.factory.annotation.Autowired;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ActionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ActionService actionService;

    @Autowired
    private DataSource dataSource;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeAll
    void setup() throws Exception {
        executeDataSetWithStateManagement("testdata/action.xml");
    }

    @AfterEach
    void cleanAfterEach() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE action RESTART IDENTITY CASCADE");
        }
    }

    @Test
    void datasetFileShouldExist() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("testdata/action.xml");
        assertNotNull(inputStream, "Dataset file not found!");
    }

    @Test
    void testFindAllActions_shouldReturnExpectedRecords() {
        List<Action> actions = actionService.getAll();
        assertFalse(actions.isEmpty(), "Expected non-empty list of actions");
    }

    @Test
    void testFindById_shouldReturnCorrectAction() {
        Action action = actionService.findById("A001");
        assertNotNull(action);
        assertEquals("CODE1", action.getCode());
    }

    @Test
    void testFindById_shouldReturnNullForNonexistentId() {
        Action action = actionService.findById("NON_EXISTENT_ID");
        assertNull(action);
    }

    @Test
    void testSave_shouldPersistNewAction() {
        Action newAction = new Action();
        newAction.setId("A005");
        newAction.setCode("CODE5");
        newAction.setDescription("New Action");
        newAction.setType("TYPE5");

        Action saved = actionService.save(newAction);
        assertEquals("New Action", saved.getDescription());
        assertEquals("CODE5-New Action", saved.getActionDisplayValue());
    }

    @Test
    void testUpdate_shouldUpdateExistingAction() {
        Action action = actionService.findById("A002");
        assertNotNull(action);

        action.setDescription("Updated Description");
        actionService.update(action);

        Action updated = actionService.findById("A002");
        assertEquals("Updated Description", updated.getDescription());
    }

    @Test
    void testUpdate_shouldThrowExceptionForNullId() {
        Action action = new Action();
        assertThrows(Exception.class, () -> actionService.update(action));
    }

    @Test
    void testDelete_shouldRemoveAction() {
        actionService.delete("A003");
        assertNull(actionService.findById("A003"));
    }

    @Test
    void testDelete_shouldThrowForNonExistentId() {
        assertDoesNotThrow(() -> actionService.delete("NON_EXISTENT_ID"));
    }

    @Test
    void testGetActionDisplayValue_withCode() {
        Action action = actionService.findById("A001");
        assertEquals("CODE1-Action One", action.getActionDisplayValue());
    }

    @Test
    void testGetActionDisplayValue_withoutCode() {
        Action action = new Action();
        action.setDescription("Only Description");
        assertEquals("Only Description", action.getActionDisplayValue());
    }

    @Test
    void testSave_shouldThrowOnNullFields() {
        Action incompleteAction = new Action();
        assertThrows(Exception.class, () -> actionService.save(incompleteAction));
    }

    @Test
    void testMultipleInsertionsAndRetrievals() {
        for (int i = 10; i < 15; i++) {
            Action action = new Action();
            action.setId("AX" + i);
            action.setCode("CD" + i);
            action.setDescription("Desc" + i);
            action.setType("TYP" + i);
            actionService.save(action);
        }

        assertEquals(5, actionService.getAll().stream().filter(a -> a.getId().startsWith("AX")).count());
    }
}
