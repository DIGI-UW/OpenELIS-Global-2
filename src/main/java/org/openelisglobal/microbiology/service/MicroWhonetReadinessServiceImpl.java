package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroWhonetReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroWhonetReadinessServiceImpl implements MicroWhonetReadinessService {

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroAstReadingDAO astReadingDAO;

    public MicroWhonetReadinessServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO,
            MicroAstReadingDAO astReadingDAO) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.astReadingDAO = astReadingDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroWhonetReadinessForm getReadiness(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroWhonetReadinessForm readiness = new MicroWhonetReadinessForm();
        readiness.caseId = caseId;
        readiness.whonetReady = true;
        List<MicroIsolate> isolates = isolateDAO.getByCaseId(caseId);
        if (isolates.isEmpty()) {
            readiness.whonetReady = false;
            readiness.blockers.add("ISOLATE_REQUIRED");
            return readiness;
        }
        for (MicroIsolate isolate : isolates) {
            if (isolate.getOrganismId() == null || isolate.getOrganismId().trim().isEmpty()) {
                addBlocker(readiness, "ORGANISM_MAPPING_REQUIRED");
            }
            if (!hasAstReading(isolate.getId())) {
                addBlocker(readiness, "AST_RESULT_REQUIRED");
            }
        }
        readiness.whonetReady = readiness.blockers.isEmpty();
        return readiness;
    }

    private boolean hasAstReading(String isolateId) {
        for (MicroAstRun run : astRunDAO.getByIsolateId(isolateId)) {
            for (MicroAstReading reading : astReadingDAO.getByRunId(run.getId())) {
                if (reading.getAntibioticId() != null && !reading.getAntibioticId().trim().isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void addBlocker(MicroWhonetReadinessForm readiness, String blocker) {
        if (!readiness.blockers.contains(blocker)) {
            readiness.blockers.add(blocker);
        }
    }
}
