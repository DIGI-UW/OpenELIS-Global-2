package org.openelisglobal.microbiology.service;

import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.form.MicroCaseReadinessForm;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroReportReleaseServiceImpl implements MicroReportReleaseService {

    private final MicroCaseDAO caseDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroCaseReadinessService readinessService;
    private final MicroCaseStateService stateService;
    private final MicroCriticalCommunicationDAO communicationDAO;

    public MicroReportReleaseServiceImpl(MicroCaseDAO caseDAO, MicroCaseActivityDAO activityDAO,
            MicroCaseReadinessService readinessService, MicroCaseStateService stateService,
            MicroCriticalCommunicationDAO communicationDAO) {
        this.caseDAO = caseDAO;
        this.activityDAO = activityDAO;
        this.readinessService = readinessService;
        this.stateService = stateService;
        this.communicationDAO = communicationDAO;
    }

    @Override
    @Transactional
    public MicroCase releasePreliminary(String caseId, String performedBy) {
        MicroCase microCase = getCase(caseId);
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.PRELIMINARY_RELEASED.name());
        microCase.setStage(MicroCaseStage.PRELIM_RELEASED.name());
        MicroCase updated = caseDAO.update(microCase);
        recordActivity(caseId, MicroCaseActivityType.PRELIMINARY_REPORT_RELEASED, performedBy,
                "Preliminary report released");
        return updated;
    }

    @Override
    @Transactional
    public MicroCase releaseFinal(String caseId, String performedBy) {
        MicroCaseReadinessForm readiness = readinessService.getReadiness(caseId);
        if (!readiness.finalReleaseReady) {
            throw new IllegalStateException("Final release is blocked: " + String.join(", ", readiness.blockers));
        }
        if (hasOpenCriticalFollowUp(caseId)) {
            throw new IllegalStateException("Final release is blocked: CRITICAL_FOLLOW_UP_REQUIRED");
        }
        MicroCase microCase = stateService.advanceStage(caseId, MicroCaseStage.FINAL_RELEASED, performedBy,
                "Final report released");
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        microCase.setClosedAt(MicroCaseServiceImpl.now());
        microCase.setClosedBy(performedBy);
        MicroCase updated = caseDAO.update(microCase);
        recordActivity(caseId, MicroCaseActivityType.FINAL_REPORT_RELEASED, performedBy, "Final report released");
        return updated;
    }

    private MicroCase getCase(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
    }

    private void recordActivity(String caseId, MicroCaseActivityType activityType, String performedBy, String note) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(activityType.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote(note);
        activityDAO.insert(activity);
    }

    private boolean hasOpenCriticalFollowUp(String caseId) {
        for (MicroCriticalCommunication communication : communicationDAO.getByCaseId(caseId)) {
            if (Boolean.TRUE.equals(communication.getFollowUpNeeded())
                    && MicroCriticalCommunicationStatus.OPEN.name().equals(communication.getAcknowledgementStatus())) {
                return true;
            }
        }
        return false;
    }
}
