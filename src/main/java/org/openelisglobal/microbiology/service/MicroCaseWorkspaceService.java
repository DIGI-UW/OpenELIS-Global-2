package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.*;

public interface MicroCaseWorkspaceService {
    MicroCaseSearchPageForm search(MicroCaseSearchForm query, String userId);

    MicroCaseShellForm get(String caseId, String userId);

    MicroCaseShellForm transfer(String caseId, String labUnitId, String userId);
}
