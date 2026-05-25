package org.openelisglobal.notebook.valueholder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * One workflow stage from the AHRI workflow registry.
 */
public class WorkflowStageDefinition {

    private final String departmentName;
    private final String workflowType;
    private final int stageOrder;
    private final String stageId;
    private final String pageKey;
    private final String stageTitle;
    private final List<String> allowedPersonas;
    private final Set<NotebookStageAction> allowedActions;

    public WorkflowStageDefinition(String departmentName, String workflowType, int stageOrder, String stageId,
            String pageKey, String stageTitle, List<String> allowedPersonas, Set<NotebookStageAction> allowedActions) {
        this.departmentName = departmentName;
        this.workflowType = workflowType;
        this.stageOrder = stageOrder;
        this.stageId = stageId;
        this.pageKey = pageKey != null && !pageKey.isBlank() ? pageKey.trim()
                : (stageId != null ? stageId.trim() : "stage-" + stageOrder);
        this.stageTitle = stageTitle;
        this.allowedPersonas = allowedPersonas == null ? List.of() : List.copyOf(allowedPersonas);
        this.allowedActions = allowedActions == null || allowedActions.isEmpty() ? NotebookStageAction.DEFAULT_STAGE_ACTIONS
                : Collections.unmodifiableSet(allowedActions);
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public int getStageOrder() {
        return stageOrder;
    }

    public String getStageId() {
        return stageId;
    }

    /** Stable key for authorization (matches notebook_page.page_id when set). */
    public String getPageKey() {
        return pageKey;
    }

    public String getStageTitle() {
        return stageTitle;
    }

    public List<String> getAllowedPersonas() {
        return Collections.unmodifiableList(allowedPersonas);
    }

    public Set<NotebookStageAction> getAllowedActions() {
        return allowedActions;
    }

    public boolean permitsAction(NotebookStageAction action) {
        return action != null && allowedActions.contains(action);
    }
}
