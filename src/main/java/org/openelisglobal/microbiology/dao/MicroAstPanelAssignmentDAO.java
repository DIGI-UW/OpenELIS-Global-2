package org.openelisglobal.microbiology.dao;

import java.util.List;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstPanelAssignment;

public interface MicroAstPanelAssignmentDAO extends BaseDAO<MicroAstPanelAssignment, String> {
    List<MicroAstPanelAssignment> getByIsolateId(String id);
}
