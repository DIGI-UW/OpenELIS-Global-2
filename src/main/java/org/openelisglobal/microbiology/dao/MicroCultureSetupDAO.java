package org.openelisglobal.microbiology.dao;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCultureSetup;

public interface MicroCultureSetupDAO extends BaseDAO<MicroCultureSetup, String> {
    List<MicroCultureSetup> getActiveSetupsByWorkflowType(String workflowType);

    MicroCultureSetup getActiveSetupForMethod(String methodId, String workflowType);

    Optional<MicroCultureSetup> findByMethodAndWorkflowType(String methodId, String workflowType);

    List<MicroCultureSetup> search(String q, String status, String workflow, String sort, int offset, int limit);

    long countSearch(String q, String status, String workflow);
}
