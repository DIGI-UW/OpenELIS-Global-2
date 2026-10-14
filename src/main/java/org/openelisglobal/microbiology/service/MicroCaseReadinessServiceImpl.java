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
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroCaseReadinessServiceImpl implements MicroCaseReadinessService {

    @org.springframework.beans.factory.annotation.Autowired
    private MicroIsolateWorkspaceServiceImpl isolateWorkspace;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroDstService dst;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroCaseFinalRequirementsService requirements;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseInoculationDAO cultures;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO links;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroAstReportChoiceService choices;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.referral.dao.ReferralDAO referrals;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.common.services.IStatusService statuses;
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
        List<MicroIsolate> isolates = isolateDAO.getWorkspace(caseId).stream().filter(i -> i.getCancelledAt() == null)
                .toList();
        var cultureRows = cultures.getRows(caseId);
        if (cultureRows.isEmpty()
                || cultureRows.stream().anyMatch(row -> row.getOutcome() == null || row.getOutcomeAt() == null))
            addBlocker(readiness, "CULTURE_OUTCOME_REQUIRED");
        for (var analysis : links.getAnalyses(caseId)) {
            var link = links.getByCaseAndAnalysis(caseId, analysis.getId());
            if (MicroPatientReportService.selected(link) && !analysis.getTest().isInLabOnly()
                    && !"N".equals(analysis.getIsReportable())
                    && !"Finalized".equals(statuses.getAnalysisStatusForID(analysis.getStatusId()).name()))
                addBlocker(readiness, "SELECTED_RESULT_VALIDATION_REQUIRED");
        }
        var sources = cultures.getSources(caseId).stream().map(si -> si.getId()).distinct().toList();
        if (referrals.getWorkspaceReferrals(sources).stream().filter(referral -> {
            var analysis = referral.getAnalysis();
            if (analysis == null)
                return false;
            var link = links.getByCaseAndAnalysis(caseId, analysis.getId());
            return link != null && !analysis.getTest().isInLabOnly() && !"N".equals(analysis.getIsReportable())
                    && (link.getCaseRole() == org.openelisglobal.microbiology.valueholder.MicroCaseRole.CULTURE
                            || MicroPatientReportService.selected(link));
        }).anyMatch(referral -> referral.getStatus() == null || !java.util.Set
                .of(org.openelisglobal.referral.valueholder.ReferralStatus.COMPLETED,
                        org.openelisglobal.referral.valueholder.ReferralStatus.CANCELLED)
                .contains(referral.getStatus())))
            addBlocker(readiness, "REFERRAL_COMPLETION_REQUIRED");
        readiness.blockers.addAll(requirements.blockers(caseId));
        readiness.blockerLabels = requirements.labels(caseId);
        if (dst.evaluate(caseId).unresolvedDiscordance)
            addBlocker(readiness, "TB_DISCORDANCE_UNRESOLVED");
        boolean noGrowthReady = !cultureRows.isEmpty() && cultureRows.stream().allMatch(row -> java.util.Set
                .of("NO_GROWTH", "CONTAMINATED").contains(row.getOutcome() == null ? "" : row.getOutcome()));
        if (isolates.isEmpty() && !noGrowthReady) {
            readiness.finalReleaseReady = false;
            readiness.blockers.add("ISOLATE_REQUIRED");
        }
        if (hasOpenCriticalFollowUp(caseId)) {
            readiness.finalReleaseReady = false;
            readiness.blockers.add("CRITICAL_FOLLOW_UP_REQUIRED");
        }
        for (MicroIsolate isolate : isolates) {
            if (isolateWorkspace.mismatch(isolate) && isolate.getMismatchAcknowledgedAt() == null) {
                readiness.finalReleaseReady = false;
                addBlocker(readiness, "REPORTED_IDENTIFICATION_MISMATCH");
            }
            List<MicroAstRun> activeRuns = activeAstRuns(isolate.getId());
            List<MicroAstRun> reviewedRuns = activeRuns.stream()
                    .filter(run -> MicroAstRunStatus.REVIEWED.name().equals(run.getStatus())).toList();
            readiness.astRunsTotal += activeRuns.size();
            readiness.astRunsComplete += reviewedRuns.size();
            boolean identified = MicroIsolateIdentificationStatus.CONFIRMED.name()
                    .equals(isolate.getIdentificationStatus()) && isolate.getOrganismId() != null
                    && !isolate.getOrganismId().isBlank();
            if (!identified) {
                readiness.isolatesPendingIdentification++;
                readiness.finalReleaseReady = false;
                addBlocker(readiness, "ISOLATE_IDENTIFICATION_REQUIRED");
            }
            boolean unreviewedWork = activeRuns.stream()
                    .anyMatch(run -> "QC_FAILED".equals(run.getStatus()) || "FAILED".equals(run.getQcState()));
            if (unreviewedWork)
                addBlocker(readiness, "AST_REVIEW_REQUIRED");
            if (MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name().equals(isolate.getSignificance())
                    && choices.getChoices(isolate.getId()).isEmpty()) {
                readiness.finalReleaseReady = false;
                addBlocker(readiness, "AST_REVIEW_REQUIRED");
                if (identified && activeRuns.isEmpty()) {
                    readiness.significantIsolatesAwaitingAstSetup++;
                }
            }
            try {
                choices.getSelectedReadings(isolate.getId());
            } catch (MicroAstConflictException invalidSelection) {
                addBlocker(readiness, "REPORTABLE_AST_READING_REQUIRED");
            }

        }
        readiness.finalReleaseReady = readiness.finalReleaseReady && readiness.blockers.isEmpty();
        return readiness;
    }

    private List<MicroAstRun> activeAstRuns(String isolateId) {
        return astRunDAO.getByIsolateId(isolateId).stream()
                .filter(run -> !MicroAstRunStatus.INVALIDATED.name().equals(run.getStatus())
                        && !MicroAstRunStatus.RERUN_REQUIRED.name().equals(run.getStatus())
                        && !MicroAstRunStatus.CANCELLED.name().equals(run.getStatus()))
                .toList();
    }

    private void addBlocker(MicroCaseReadinessForm readiness, String blocker) {
        if (!readiness.blockers.contains(blocker)) {
            readiness.blockers.add(blocker);
        }
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
