package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;

public interface MicroAstPanelDAO extends BaseDAO<MicroAstPanel, String> {
    List<MicroAstPanel> getActivePanelsByWorkflowType(String workflowType);
}
