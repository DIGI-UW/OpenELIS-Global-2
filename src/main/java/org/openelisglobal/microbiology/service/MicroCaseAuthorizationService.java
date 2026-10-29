package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseAuthorizationService {
    private final MicroCaseDAO cases;
    private final MicrobiologyCaseAccessService access;

    public MicroCaseAuthorizationService(MicroCaseDAO cases, MicrobiologyCaseAccessService access) {
        this.cases = cases;
        this.access = access;
    }

    @Transactional
    public MicroCase requireWrite(String caseId, String actor, String role) {
        var c = cases.getForUpdate(caseId);
        if (c == null)
            throw new IllegalArgumentException("Case not found");
        if (actor == null || !access.hasLabUnitRole(actor, c.getLabUnitId(), role))
            throw new AccessDeniedException("Case lab unit access required");
        MicroCaseMutationGuard.requireMutable(c);
        return c;
    }
}
