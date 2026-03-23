package org.openelisglobal.coldstorage;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.coldstorage.service.CorrectiveActionService;
import org.openelisglobal.coldstorage.valueholder.CorrectiveAction;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionStatus;
import org.openelisglobal.coldstorage.valueholder.CorrectiveActionType;
import org.springframework.beans.factory.annotation.Autowired;

public class CorrectiveActionServiceTest extends BaseWebContextSensitiveTest {

    @Autowired
    private CorrectiveActionService correctiveActionService;

    private Long testFreezerId = 100L;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/corrective_action_service.xml");
    }

    @Test
    public void testCreateCorrectiveAction_WithValidData_CreatesActionWithPendingStatus() {
        CorrectiveAction result = correctiveActionService.createCorrectiveAction(testFreezerId,
                CorrectiveActionType.TEMPERATURE_ADJUSTMENT, "Adjust thermostat to -20°C", 1);

        assertNotNull("Corrective action should not be null", result);
        assertNotNull("Corrective action ID should not be null", result.getId());
        assertEquals("Status should be PENDING", CorrectiveActionStatus.PENDING, result.getStatus());
        assertEquals("Action type should be TEMPERATURE_ADJUSTMENT", CorrectiveActionType.TEMPERATURE_ADJUSTMENT,
                result.getActionType());
        assertEquals("Description should match", "Adjust thermostat to -20°C", result.getDescription());
        assertNotNull("Created at should not be null", result.getCreatedAt());
        assertNotNull("Created by should not be null", result.getCreatedBy());
        assertEquals("Created by user ID should be 1", "1", result.getCreatedBy().getId());
    }

    @Test
    public void testUpdateCorrectiveAction_WithPendingAction_TransitionsToInProgress() {
        CorrectiveAction action = correctiveActionService.createCorrectiveAction(testFreezerId,
                CorrectiveActionType.EQUIPMENT_REPAIR, "Repair compressor", 1);

        CorrectiveAction result = correctiveActionService.updateCorrectiveActionStatus(action.getId(),
                CorrectiveActionStatus.IN_PROGRESS, 1);

        assertNotNull("Corrective action should not be null", result);
        assertEquals("Status should be IN_PROGRESS", CorrectiveActionStatus.IN_PROGRESS, result.getStatus());
        assertNotNull("Updated at should not be null", result.getUpdatedAt());
    }

    @Test
    public void testGetCorrectiveActionsByStatus_WithPendingStatus_ReturnsPendingActions() {
        CorrectiveAction pendingAction = correctiveActionService.createCorrectiveAction(testFreezerId,
                CorrectiveActionType.TEMPERATURE_ADJUSTMENT, "Pending action", 1);

        CorrectiveAction inProgressAction = correctiveActionService.createCorrectiveAction(testFreezerId,
                CorrectiveActionType.EQUIPMENT_REPAIR, "In progress action", 1);
        correctiveActionService.updateCorrectiveActionStatus(inProgressAction.getId(),
                CorrectiveActionStatus.IN_PROGRESS, 1);

        List<CorrectiveAction> result = correctiveActionService
                .getCorrectiveActionsByStatus(CorrectiveActionStatus.PENDING);

        assertNotNull("Result should not be null", result);
        assertTrue("Should have at least 1 pending action", result.size() >= 1);
        for (CorrectiveAction action : result) {
            assertEquals("All actions should be PENDING", CorrectiveActionStatus.PENDING, action.getStatus());
        }
    }

    @Test
    public void testCompleteCorrectiveAction_WithInProgressAction_TransitionsToCompleted() {
        CorrectiveAction action = correctiveActionService.createCorrectiveAction(testFreezerId,
                CorrectiveActionType.TEMPERATURE_ADJUSTMENT, "Adjust thermostat", 1);
        action = correctiveActionService.updateCorrectiveActionStatus(action.getId(),
                CorrectiveActionStatus.IN_PROGRESS, 1);

        CorrectiveAction result = correctiveActionService.completeCorrectiveAction(action.getId(), 1,
                "Temperature stabilized");

        assertNotNull("Corrective action should not be null", result);
        assertEquals("Status should be COMPLETED", CorrectiveActionStatus.COMPLETED, result.getStatus());
        assertNotNull("Completed at should not be null", result.getCompletedAt());
        assertEquals("Completion notes should match", "Temperature stabilized", result.getCompletionNotes());
    }
}
