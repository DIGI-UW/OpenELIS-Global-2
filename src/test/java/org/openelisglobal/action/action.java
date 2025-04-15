package org.openelisglobal.action;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.*;
import org.openelisglobal.action.service.ActionService;
import org.openelisglobal.action.valueholder.Action;
import org.openelisglobal.BaseWebContextSensitiveTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
public class ActionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private ActionService actionService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    @BeforeEach
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/action.xml");
    }

    @AfterEach
    public void cleanUp() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("TRUNCATE TABLE action RESTART IDENTITY CASCADE");
        }
    }

    @Test
    public void testDatasetLoadedSuccessfully() {
        List<Action> actions = actionService.getAll();
        assertNotNull(actions);
        assertEquals(4, actions.size());
    }

    @Test
    public void testFindById_shouldReturnCorrectAction() {
        Action action = actionService.findById("A001");
        assertNotNull(action);
        assertEquals("CODE1", action.getCode());
    }

    @Test
    public void testSave_shouldPersistNewAction() {
        Action newAction = new Action();
        newAction.setId("A005");
        newAction.setCode("NEWCODE");
        newAction.setDescription("New Action");
        newAction.setType("NEWTYPE");

        actionService.save(newAction);

        Action saved = actionService.findById("A005");
        assertNotNull(saved);
        assertEquals("NEWCODE", saved.getCode());
    }

    @Test
    public void testUpdate_shouldChangeFieldsCorrectly() {
        Action action = actionService.findById("A002");
        assertNotNull(action);

        action.setDescription("Updated Desc");
        action.setType("UPDATED_TYPE");
        actionService.update(action);

        Action updated = actionService.findById("A002");
        assertEquals("Updated Desc", updated.getDescription());
        assertEquals("UPDATED_TYPE", updated.getType());
    }

    @Test
    public void testDelete_shouldRemoveAction() {
        actionService.delete("A003");
        assertNull(actionService.findById("A003"));
    }

    @Test
    public void testGetActionDisplayValue_whenCodePresent() {
        Action action = actionService.findById("A001");
        String display = action.getActionDisplayValue();
        assertEquals("CODE1-Action One", display);
    }

    @Test
    public void testGetActionDisplayValue_whenCodeNull() {
        Action action = actionService.findById("A001");
        action.setCode(null);
        String display = action.getActionDisplayValue();
        assertEquals("Action One", display);
    }

    @Test
    public void testDeleteNonExistent_shouldNotThrow() {
        assertDoesNotThrow(() -> actionService.delete("NON_EXISTENT"));
    }

    @Test
    public void testFindById_invalidIdShouldReturnNull() {
        assertNull(actionService.findById("INVALID"));
    }

    @Test
    public void testSave_duplicateId_shouldOverwrite() {
        Action existing = actionService.findById("A001");
        assertNotNull(existing);

        existing.setDescription("Overwritten Desc");
        actionService.save(existing);

        Action updated = actionService.findById("A001");
        assertEquals("Overwritten Desc", updated.getDescription());
    }
}
