package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.form.*;

public interface MicroIsolateWorkspaceService {
    MicroIsolateWorkspaceForm get(String caseId, String actor);

    MicroIsolateWorkspaceForm.Options options(String caseId);

    MicroIsolateWorkspaceForm pick(String caseId, MicroIsolateWorkspaceRequestForm request, String actor);

    MicroIsolateWorkspaceForm act(String caseId, String isolateId, String action,
            MicroIsolateWorkspaceRequestForm request, String actor);

    MicroIsolateWorkspaceForm refer(String caseId, MicroIsolateWorkspaceRequestForm request, String actor);
}
