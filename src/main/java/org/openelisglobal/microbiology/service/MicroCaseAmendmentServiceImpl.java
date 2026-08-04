package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.List;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseActivityDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAmendmentDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivity;
import org.openelisglobal.microbiology.valueholder.MicroCaseActivityType;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendment;
import org.openelisglobal.microbiology.valueholder.MicroCaseAmendmentStatus;
import org.openelisglobal.microbiology.valueholder.MicroCaseFinalReleaseState;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseAmendmentServiceImpl implements MicroCaseAmendmentService {

    private final MicroCaseDAO caseDAO;
    private final MicroCaseAmendmentDAO amendmentDAO;
    private final MicroCaseActivityDAO activityDAO;
    private final MicroReportVersionService reportVersionService;
    private final MicroAstRunDAO astRunDAO;
    private final MicroIdentificationHistoryService identificationHistoryService;

    public MicroCaseAmendmentServiceImpl(MicroCaseDAO caseDAO, MicroCaseAmendmentDAO amendmentDAO,
            MicroCaseActivityDAO activityDAO, MicroReportVersionService reportVersionService, MicroAstRunDAO astRunDAO,
            MicroIdentificationHistoryService identificationHistoryService) {
        this.caseDAO = caseDAO;
        this.amendmentDAO = amendmentDAO;
        this.activityDAO = activityDAO;
        this.reportVersionService = reportVersionService;
        this.astRunDAO = astRunDAO;
        this.identificationHistoryService = identificationHistoryService;
    }

    @Override
    @Transactional
    public MicroCaseAmendment openAmendment(String caseId, String reason, String performedBy) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        requireReason(reason);
        MicroCase microCase = getCase(caseId);
        if (amendmentDAO.getOpenByCaseId(caseId) != null) {
            throw new MicroAmendmentConflictException("AMENDMENT_ALREADY_OPEN");
        }
        if (!MicroCaseStage.FINAL_RELEASED.name().equals(microCase.getStage())
                || !MicroCaseFinalReleaseState.FINAL_RELEASED.name().equals(microCase.getFinalReleaseState())) {
            throw new MicroAmendmentConflictException("AMENDMENT_REQUIRES_FINAL_RELEASE");
        }

        reportVersionService.ensureFinalBaseline(microCase);
        Timestamp now = MicroCaseServiceImpl.now();
        MicroCaseAmendment amendment = new MicroCaseAmendment();
        amendment.setCaseId(caseId);
        amendment.setSequenceNumber(amendmentDAO.getNextSequence(caseId));
        amendment.setStatus(MicroCaseAmendmentStatus.OPEN.name());
        amendment.setReason(reason.trim());
        amendment.setOpenedAt(now);
        amendment.setOpenedBy(performedBy);
        amendmentDAO.insert(amendment);

        microCase.setStage(MicroCaseStage.AMENDED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name());
        microCase.setClosedAt(null);
        microCase.setClosedBy(null);
        caseDAO.update(microCase);
        recordActivity(caseId, MicroCaseActivityType.AMENDMENT_OPENED, performedBy,
                "Amendment " + amendment.getSequenceNumber() + " opened", amendment.getId());
        return amendment;
    }

    @Override
    @Transactional
    public MicroCaseAmendment completeAmendment(String caseId, MicroReportProjectionResult projection,
            String performedBy) {
        MicroCase microCase = requireActiveAmendmentCase(caseId);
        MicroCaseAmendment amendment = requireOpenAmendment(caseId);
        reportVersionService.recordAmendedFinal(amendment, projection, performedBy);
        close(amendment, MicroCaseAmendmentStatus.RELEASED, null, performedBy);
        relock(microCase, performedBy);
        recordActivity(caseId, MicroCaseActivityType.AMENDMENT_RELEASED, performedBy,
                "Amendment " + amendment.getSequenceNumber() + " released", amendment.getId());
        return amendment;
    }

    @Override
    @Transactional
    public MicroCaseAmendment cancelAmendment(String caseId, String reason, String performedBy) {
        requireReason(reason);
        MicroCase microCase = requireActiveAmendmentCase(caseId);
        MicroCaseAmendment amendment = requireOpenAmendment(caseId);
        identificationHistoryService.revertAmendment(amendment.getId(), reason.trim(), performedBy);
        for (MicroAstRun run : astRunDAO.getByAmendmentId(amendment.getId())) {
            run.setStatus(MicroAstRunStatus.CANCELLED.name());
            astRunDAO.update(run);
        }
        close(amendment, MicroCaseAmendmentStatus.CANCELLED, reason.trim(), performedBy);
        relock(microCase, performedBy);
        recordActivity(caseId, MicroCaseActivityType.AMENDMENT_CANCELLED, performedBy,
                "Amendment " + amendment.getSequenceNumber() + " cancelled", amendment.getId());
        return amendment;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroCaseAmendment getOpenAmendment(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return amendmentDAO.getOpenByCaseId(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MicroCaseAmendment> getHistory(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return amendmentDAO.getByCaseId(caseId);
    }

    private MicroCase getCase(String caseId) {
        return caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
    }

    private MicroCase requireActiveAmendmentCase(String caseId) {
        MicroCase microCase = getCase(caseId);
        if (!MicroCaseStage.AMENDED.name().equals(microCase.getStage())
                || !MicroCaseFinalReleaseState.AMENDMENT_IN_PROGRESS.name().equals(microCase.getFinalReleaseState())) {
            throw new MicroAmendmentConflictException("AMENDMENT_NOT_OPEN");
        }
        return microCase;
    }

    private MicroCaseAmendment requireOpenAmendment(String caseId) {
        MicroCaseAmendment amendment = amendmentDAO.getOpenByCaseId(caseId);
        if (amendment == null) {
            throw new MicroAmendmentConflictException("AMENDMENT_NOT_OPEN");
        }
        return amendment;
    }

    private void close(MicroCaseAmendment amendment, MicroCaseAmendmentStatus status, String closingReason,
            String performedBy) {
        amendment.setStatus(status.name());
        amendment.setClosedAt(MicroCaseServiceImpl.now());
        amendment.setClosedBy(performedBy);
        amendment.setClosingReason(closingReason);
        amendmentDAO.update(amendment);
    }

    private void relock(MicroCase microCase, String performedBy) {
        microCase.setStage(MicroCaseStage.FINAL_RELEASED.name());
        microCase.setFinalReleaseState(MicroCaseFinalReleaseState.FINAL_RELEASED.name());
        microCase.setClosedAt(MicroCaseServiceImpl.now());
        microCase.setClosedBy(performedBy);
        caseDAO.update(microCase);
    }

    private void requireReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("AMENDMENT_REASON_REQUIRED");
        }
    }

    private void recordActivity(String caseId, MicroCaseActivityType activityType, String performedBy, String note,
            String amendmentId) {
        MicroCaseActivity activity = new MicroCaseActivity();
        activity.setCaseId(caseId);
        activity.setActivityType(activityType.name());
        activity.setOccurredAt(MicroCaseServiceImpl.now());
        activity.setPerformedBy(performedBy);
        activity.setNote(note);
        activity.setStructuredData("{\"amendmentId\":\"" + amendmentId + "\"}");
        activityDAO.insert(activity);
    }
}
