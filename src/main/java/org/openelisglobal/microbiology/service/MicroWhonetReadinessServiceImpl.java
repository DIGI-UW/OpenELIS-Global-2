package org.openelisglobal.microbiology.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.form.MicroWhonetReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroWhonetReadinessServiceImpl implements MicroWhonetReadinessService {

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroAstReadingDAO astReadingDAO;
    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;

    public MicroWhonetReadinessServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO,
            MicroAstReadingDAO astReadingDAO, MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.astReadingDAO = astReadingDAO;
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
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
            } else if (!hasWhonetOrganismCode(isolate.getOrganismId())) {
                addBlocker(readiness, "ORGANISM_MAPPING_REQUIRED");
            }
            if (!hasWhonetReadyAstReading(readiness, isolate.getId())) {
                addBlocker(readiness, "AST_RESULT_REQUIRED");
            }
        }
        readiness.whonetReady = readiness.blockers.isEmpty();
        return readiness;
    }

    private boolean hasWhonetReadyAstReading(MicroWhonetReadinessForm readiness, String isolateId) {
        boolean hasAstReading = false;
        for (MicroAstRun run : astRunDAO.getByIsolateId(isolateId)) {
            for (MicroAstReading reading : astReadingDAO.getByRunId(run.getId())) {
                if (reading.getAntibioticId() != null && !reading.getAntibioticId().trim().isEmpty()) {
                    hasAstReading = true;
                    if (!hasWhonetAntibioticCode(reading.getAntibioticId())) {
                        addBlocker(readiness, "ANTIBIOTIC_MAPPING_REQUIRED");
                    }
                }
            }
        }
        return hasAstReading;
    }

    private boolean hasWhonetOrganismCode(String organismId) {
        Optional<MicroOrganism> organism = organismDAO.get(organismId);
        return organism.isPresent() && hasText(organism.get().getWhonetCode());
    }

    private boolean hasWhonetAntibioticCode(String antibioticId) {
        Optional<MicroAntibiotic> antibiotic = antibioticDAO.get(antibioticId);
        return antibiotic.isPresent() && hasText(antibiotic.get().getWhonetCode());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void addBlocker(MicroWhonetReadinessForm readiness, String blocker) {
        if (!readiness.blockers.contains(blocker)) {
            readiness.blockers.add(blocker);
        }
    }
}
