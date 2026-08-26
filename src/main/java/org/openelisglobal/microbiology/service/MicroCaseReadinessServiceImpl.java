package org.openelisglobal.microbiology.service;

import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseReadinessServiceImpl implements MicroCaseReadinessService {

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroCriticalCommunicationDAO communicationDAO;

    public MicroCaseReadinessServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO,
            MicroCriticalCommunicationDAO communicationDAO) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.communicationDAO = communicationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseReadinessForm getReadiness(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        MicroCaseReadinessForm readiness = new MicroCaseReadinessForm();
        readiness.caseId = microCase.getId();
        readiness.finalReleaseReady = true;
        List<MicroIsolate> isolates = isolateDAO.getByCaseId(caseId);
        boolean noGrowthReady = "NO_GROWTH_READY".equals(microCase.getStage());
        if (isolates.isEmpty() && !noGrowthReady) {
            readiness.finalReleaseReady = false;
            readiness.blockers.add("ISOLATE_REQUIRED");
        }
        if (hasOpenCriticalFollowUp(caseId)) {
            readiness.finalReleaseReady = false;
            readiness.blockers.add("CRITICAL_FOLLOW_UP_REQUIRED");
        }
        for (MicroIsolate isolate : isolates) {
            List<MicroAstRun> reviewedRuns = reviewedAstRuns(isolate.getId());
            if (MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name().equals(isolate.getSignificance())
                    && reviewedRuns.isEmpty()) {
                readiness.finalReleaseReady = false;
                if (!readiness.blockers.contains("AST_REVIEW_REQUIRED")) {
                    readiness.blockers.add("AST_REVIEW_REQUIRED");
                }
            }
            if (reviewedRuns.size() > 1 && reviewedRuns.stream().filter(MicroAstRun::isReportable).count() != 1) {
                readiness.finalReleaseReady = false;
                if (!readiness.blockers.contains("REPORTABLE_AST_RUN_REQUIRED")) {
                    readiness.blockers.add("REPORTABLE_AST_RUN_REQUIRED");
                }
            }
        }
        return readiness;
    }

    private List<MicroAstRun> reviewedAstRuns(String isolateId) {
        return astRunDAO.getByIsolateId(isolateId).stream()
                .filter(run -> MicroAstRunStatus.REVIEWED.name().equals(run.getStatus())).toList();
    }

    private boolean hasOpenCriticalFollowUp(String caseId) {
        for (MicroCriticalCommunication communication : communicationDAO.getByCaseId(caseId)) {
            if (Boolean.TRUE.equals(communication.getFollowUpNeeded()) && !MicroCriticalCommunicationStatus.CLOSED
                    .name().equals(communication.getAcknowledgementStatus())) {
                return true;
            }
        }
        return false;
    }
}
