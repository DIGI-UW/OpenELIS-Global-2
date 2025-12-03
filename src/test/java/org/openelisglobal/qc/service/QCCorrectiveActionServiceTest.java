package org.openelisglobal.qc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qc.dao.QCCorrectiveActionDAO;
import org.openelisglobal.qc.valueholder.QCCorrectiveAction;

/**
 * Unit tests for QCCorrectiveActionService (T137). Tests state machine
 * transitions: PENDING → ASSIGNED → IN_PROGRESS → COMPLETED
 */
@RunWith(MockitoJUnitRunner.class)
public class QCCorrectiveActionServiceTest {

    @Mock
    private QCCorrectiveActionDAO correctiveActionDAO;

    @Mock
    private QCRuleViolationService violationService;

    @InjectMocks
    private QCCorrectiveActionServiceImpl correctiveActionService;

    private QCCorrectiveAction createAction(String id, String status) {
        QCCorrectiveAction action = new QCCorrectiveAction();
        action.setId(id);
        action.setViolationId("V1");
        action.setActionType(QCCorrectiveActionService.ACTION_TYPE_RECALIBRATION);
        action.setDescription("Test action");
        action.setCreatedByUserId(1);
        action.setCreatedDateTime(Timestamp.from(Instant.now()));
        action.setStatus(status);
        return action;
    }

    // ==================== createAction Tests ====================

    @Test
    public void createAction_validInput_createsActionWithPendingStatus() {
        when(correctiveActionDAO.insert(any(QCCorrectiveAction.class))).thenReturn("A1");

        QCCorrectiveAction result = correctiveActionService.createAction("V1",
                QCCorrectiveActionService.ACTION_TYPE_RECALIBRATION, "Recalibrate instrument", 1);

        assertNotNull(result);
        assertEquals("A1", result.getId());
        assertEquals("V1", result.getViolationId());
        assertEquals(QCCorrectiveActionService.ACTION_TYPE_RECALIBRATION, result.getActionType());
        assertEquals("Recalibrate instrument", result.getDescription());
        assertEquals(QCCorrectiveActionService.STATUS_PENDING, result.getStatus());
        assertNotNull(result.getCreatedDateTime());

        verify(correctiveActionDAO).insert(any(QCCorrectiveAction.class));
    }

    @Test
    public void createAction_allActionTypes_accepted() {
        when(correctiveActionDAO.insert(any(QCCorrectiveAction.class))).thenReturn("A1");

        String[] actionTypes = {QCCorrectiveActionService.ACTION_TYPE_RECALIBRATION,
                QCCorrectiveActionService.ACTION_TYPE_MAINTENANCE, QCCorrectiveActionService.ACTION_TYPE_REPEAT_CONTROL,
                QCCorrectiveActionService.ACTION_TYPE_REAGENT_CHANGE, QCCorrectiveActionService.ACTION_TYPE_OTHER};

        for (String actionType : actionTypes) {
            QCCorrectiveAction result = correctiveActionService.createAction("V1", actionType, "Test", 1);
            assertEquals(actionType, result.getActionType());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAction_nullViolationId_throwsException() {
        correctiveActionService.createAction(null, QCCorrectiveActionService.ACTION_TYPE_RECALIBRATION, "Test", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAction_emptyDescription_throwsException() {
        correctiveActionService.createAction("V1", QCCorrectiveActionService.ACTION_TYPE_RECALIBRATION, "", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAction_invalidActionType_throwsException() {
        correctiveActionService.createAction("V1", "INVALID_TYPE", "Test", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createAction_nullUserId_throwsException() {
        correctiveActionService.createAction("V1", QCCorrectiveActionService.ACTION_TYPE_RECALIBRATION, "Test", null);
    }

    // ==================== assignAction Tests ====================

    @Test
    public void assignAction_pendingAction_transitionsToAssigned() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_PENDING);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        QCCorrectiveAction result = correctiveActionService.assignAction("A1", 2);

        assertEquals(QCCorrectiveActionService.STATUS_ASSIGNED, result.getStatus());
        assertEquals(Integer.valueOf(2), result.getAssignedUserId());
        verify(correctiveActionDAO).update(action);
    }

    @Test(expected = IllegalStateException.class)
    public void assignAction_alreadyAssigned_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_ASSIGNED);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.assignAction("A1", 2);
    }

    @Test(expected = IllegalStateException.class)
    public void assignAction_inProgress_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_IN_PROGRESS);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.assignAction("A1", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void assignAction_actionNotFound_throwsException() {
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.empty());

        correctiveActionService.assignAction("A1", 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void assignAction_nullUserId_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_PENDING);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.assignAction("A1", null);
    }

    // ==================== startAction Tests ====================

    @Test
    public void startAction_assignedAction_transitionsToInProgress() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_ASSIGNED);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        QCCorrectiveAction result = correctiveActionService.startAction("A1");

        assertEquals(QCCorrectiveActionService.STATUS_IN_PROGRESS, result.getStatus());
        assertNotNull(result.getStartedDateTime());
        verify(correctiveActionDAO).update(action);
    }

    @Test(expected = IllegalStateException.class)
    public void startAction_pendingAction_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_PENDING);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.startAction("A1");
    }

    @Test(expected = IllegalStateException.class)
    public void startAction_completedAction_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_COMPLETED);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.startAction("A1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void startAction_actionNotFound_throwsException() {
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.empty());

        correctiveActionService.startAction("A1");
    }

    // ==================== completeAction Tests ====================

    @Test
    public void completeAction_inProgressAction_transitionsToCompletedAndResolvesViolation() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_IN_PROGRESS);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        QCCorrectiveAction result = correctiveActionService.completeAction("A1", "Issue resolved by recalibration", 3);

        assertEquals(QCCorrectiveActionService.STATUS_COMPLETED, result.getStatus());
        assertEquals("Issue resolved by recalibration", result.getResolutionNotes());
        assertNotNull(result.getCompletedDateTime());
        verify(correctiveActionDAO).update(action);
        verify(violationService).resolveViolation(eq("V1"), eq(3), eq("Issue resolved by recalibration"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void completeAction_emptyResolutionNotes_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_IN_PROGRESS);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.completeAction("A1", "", 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void completeAction_nullResolutionNotes_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_IN_PROGRESS);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.completeAction("A1", null, 3);
    }

    @Test(expected = IllegalStateException.class)
    public void completeAction_pendingAction_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_PENDING);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.completeAction("A1", "Notes", 3);
    }

    @Test(expected = IllegalStateException.class)
    public void completeAction_assignedAction_throwsException() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_ASSIGNED);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));

        correctiveActionService.completeAction("A1", "Notes", 3);
    }

    @Test
    public void completeAction_violationResolutionFails_actionStillCompleted() {
        QCCorrectiveAction action = createAction("A1", QCCorrectiveActionService.STATUS_IN_PROGRESS);
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(action));
        when(violationService.resolveViolation(anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("Resolution failed"));

        // Should not throw, just log warning
        QCCorrectiveAction result = correctiveActionService.completeAction("A1", "Resolution notes", 3);

        assertEquals(QCCorrectiveActionService.STATUS_COMPLETED, result.getStatus());
        verify(correctiveActionDAO).update(action);
    }

    // ==================== Query Tests ====================

    @Test
    public void findByViolation_returnsList() {
        List<QCCorrectiveAction> expected = Arrays.asList(createAction("A1", QCCorrectiveActionService.STATUS_PENDING));
        when(correctiveActionDAO.findByViolation("V1")).thenReturn(expected);

        List<QCCorrectiveAction> result = correctiveActionService.findByViolation("V1");

        assertEquals(1, result.size());
        verify(correctiveActionDAO).findByViolation("V1");
    }

    @Test
    public void findByAssignedUser_returnsList() {
        List<QCCorrectiveAction> expected = Arrays
                .asList(createAction("A1", QCCorrectiveActionService.STATUS_ASSIGNED));
        when(correctiveActionDAO.findByAssignedUser(1)).thenReturn(expected);

        List<QCCorrectiveAction> result = correctiveActionService.findByAssignedUser(1);

        assertEquals(1, result.size());
        verify(correctiveActionDAO).findByAssignedUser(1);
    }

    @Test
    public void getIncompleteCount_sumsAllNonCompletedStatuses() {
        when(correctiveActionDAO.findByStatus(QCCorrectiveActionService.STATUS_PENDING))
                .thenReturn(Arrays.asList(createAction("A1", QCCorrectiveActionService.STATUS_PENDING)));
        when(correctiveActionDAO.findByStatus(QCCorrectiveActionService.STATUS_ASSIGNED))
                .thenReturn(Arrays.asList(createAction("A2", QCCorrectiveActionService.STATUS_ASSIGNED),
                        createAction("A3", QCCorrectiveActionService.STATUS_ASSIGNED)));
        when(correctiveActionDAO.findByStatus(QCCorrectiveActionService.STATUS_IN_PROGRESS))
                .thenReturn(Collections.emptyList());

        int count = correctiveActionService.getIncompleteCount();

        assertEquals(3, count);
    }

    @Test
    public void hasIncompleteActions_withPendingAction_returnsTrue() {
        List<QCCorrectiveAction> actions = Arrays.asList(createAction("A1", QCCorrectiveActionService.STATUS_PENDING));
        when(correctiveActionDAO.findByViolation("V1")).thenReturn(actions);

        boolean result = correctiveActionService.hasIncompleteActions("V1");

        assertTrue(result);
    }

    @Test
    public void hasIncompleteActions_allCompleted_returnsFalse() {
        List<QCCorrectiveAction> actions = Arrays
                .asList(createAction("A1", QCCorrectiveActionService.STATUS_COMPLETED));
        when(correctiveActionDAO.findByViolation("V1")).thenReturn(actions);

        boolean result = correctiveActionService.hasIncompleteActions("V1");

        assertFalse(result);
    }

    @Test
    public void hasIncompleteActions_noActions_returnsFalse() {
        when(correctiveActionDAO.findByViolation("V1")).thenReturn(Collections.emptyList());

        boolean result = correctiveActionService.hasIncompleteActions("V1");

        assertFalse(result);
    }

    // ==================== State Machine Full Workflow Test ====================

    @Test
    public void fullWorkflow_pendingToCompleted() {
        // Create
        when(correctiveActionDAO.insert(any(QCCorrectiveAction.class))).thenReturn("A1");
        QCCorrectiveAction created = correctiveActionService.createAction("V1",
                QCCorrectiveActionService.ACTION_TYPE_MAINTENANCE, "Perform maintenance", 1);
        assertEquals(QCCorrectiveActionService.STATUS_PENDING, created.getStatus());

        // Assign
        created.setId("A1");
        when(correctiveActionDAO.get("A1")).thenReturn(Optional.of(created));
        QCCorrectiveAction assigned = correctiveActionService.assignAction("A1", 2);
        assertEquals(QCCorrectiveActionService.STATUS_ASSIGNED, assigned.getStatus());

        // Start
        QCCorrectiveAction started = correctiveActionService.startAction("A1");
        assertEquals(QCCorrectiveActionService.STATUS_IN_PROGRESS, started.getStatus());

        // Complete
        QCCorrectiveAction completed = correctiveActionService.completeAction("A1", "Maintenance completed", 2);
        assertEquals(QCCorrectiveActionService.STATUS_COMPLETED, completed.getStatus());
    }
}
