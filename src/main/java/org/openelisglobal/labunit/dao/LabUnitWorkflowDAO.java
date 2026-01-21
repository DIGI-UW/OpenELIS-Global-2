package org.openelisglobal.labunit.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.labunit.valueholder.LabUnitWorkflow;

public interface LabUnitWorkflowDAO extends BaseDAO<LabUnitWorkflow, String> {

    List<LabUnitWorkflow> getWorkflowsByLabUnitId(String labUnitId);

    List<LabUnitWorkflow> getLabUnitsByWorkflowId(String workflowId);

    LabUnitWorkflow getByLabUnitAndWorkflowId(String labUnitId, String workflowId);

    void deleteByLabUnitId(String labUnitId);

    void deleteByWorkflowId(String workflowId);

    List<LabUnitWorkflow> getDefaultWorkflows(String labUnitId);

    void updateDefaultWorkflow(String labUnitId, String workflowId);

    void clearDefaultWorkflows(String labUnitId);
}