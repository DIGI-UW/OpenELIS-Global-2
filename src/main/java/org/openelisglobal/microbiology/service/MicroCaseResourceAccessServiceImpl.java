package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Resolves retained resource ownership within a service transaction. */
@Service
@Transactional(readOnly = true)
public class MicroCaseResourceAccessServiceImpl implements MicroCaseResourceAccessService {
    private final MicrobiologyCaseAccessService access;
    private final MicroCaseDAO cases;
    private final MicroIsolateDAO isolates;
    private final MicroAstRunDAO runs;
    private final MicroAstReadingDAO readings;
    private final MicroCriticalCommunicationDAO communications;

    public MicroCaseResourceAccessServiceImpl(MicrobiologyCaseAccessService access, MicroCaseDAO cases,
            MicroIsolateDAO isolates, MicroAstRunDAO runs, MicroAstReadingDAO readings,
            MicroCriticalCommunicationDAO communications) {
        this.access = access;
        this.cases = cases;
        this.isolates = isolates;
        this.runs = runs;
        this.readings = readings;
        this.communications = communications;
    }

    @Override
    public void requireAccess(String caseId, String actor, String role, boolean read) {
        var c = cases.get(caseId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!read && !access.hasLabUnitRole(actor, c.getLabUnitId(), role)) {
            throw new AccessDeniedException("Case lab unit access required");
        }
    }

    @Override
    public String resolveCaseId(String kind, String id) {
        if (id == null) {
            return null;
        }
        return switch (kind) {
        case "isolateId" ->
            isolates.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getCaseId();
        case "runId", "sourceRunId" -> resolveCaseId("isolateId",
                runs.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getIsolateId());
        case "readingId" -> resolveCaseId("runId",
                readings.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getAstRunId());
        case "communicationId" ->
            communications.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).getCaseId();
        default -> null;
        };
    }
}
