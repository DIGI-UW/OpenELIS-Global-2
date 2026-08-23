package org.openelisglobal.resultvalidation.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.audittrail.dao.AuditTrailService;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.ResultSaveService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.notification.service.TestNotificationService;
import org.openelisglobal.notification.valueholder.NotificationConfigOption.NotificationNature;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.QcEvaluation;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultvalidation.bean.AnalysisItem;
import org.openelisglobal.resultvalidation.dao.ValidationQcAcknowledgmentDAO;
import org.openelisglobal.resultvalidation.exception.QcAcknowledgmentRequiredException;
import org.openelisglobal.resultvalidation.valueholder.ValidationQcAcknowledgment;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultValidationServiceImpl implements ResultValidationService {

    private AnalysisService analysisService;
    private ResultService resultService;
    private NoteService noteService;
    private SampleService sampleService;
    private TestNotificationService testNotificationService;

    @Autowired
    private ValidationQcAcknowledgmentDAO qcAcknowledgmentDAO;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private org.openelisglobal.qc.dao.SampleItemQcProfileDAO sampleItemQcProfileDAO;

    public ResultValidationServiceImpl(AnalysisService analysisService, ResultService resultService,
            NoteService noteService, SampleService sampleService, TestNotificationService testNotificationService) {
        this.analysisService = analysisService;
        this.resultService = resultService;
        this.noteService = noteService;
        this.sampleService = sampleService;
        this.testNotificationService = testNotificationService;
    }

    @Override
    @Transactional
    public void persistdata(List<Result> deletableList, List<Analysis> analysisUpdateList,
            ArrayList<Result> resultUpdateList, List<AnalysisItem> resultItemList, ArrayList<Sample> sampleUpdateList,
            ArrayList<Note> noteUpdateList, IResultSaveService resultSaveService, List<IResultUpdate> updaters,
            String sysUserId) {
        ResultSaveService.removeDeletedResultsInTransaction(deletableList, sysUserId);

        // S-08 FR-04 release gate: refuse to release any analysis in this batch if its
        // batch (same sample) has failed QC samples without an acknowledgment row.
        enforceQcAcknowledgment(analysisUpdateList);

        // Once the client analyses are about to be released, transition any sibling QC
        // analyses on the same sample to Finalized too — leaves released_date null so
        // they aren't mistaken for released patient results, but lets the sample roll
        // up
        // to "Finished" via the existing finished-status check below.
        transitionQcAnalysesInBatch(analysisUpdateList, sysUserId);

        // update analysis
        for (Analysis analysis : analysisUpdateList) {
            analysisService.update(analysis);
        }

        for (Result resultUpdate : resultUpdateList) {
            if (resultUpdate.getId() != null) {
                resultService.update(resultUpdate);
            } else {
                LogEvent.logWarn(this.getClass().getSimpleName(), "persistdata",
                        "validating a result that doesn't exist yet. Creating result.");
                String id = resultService.insert(resultUpdate);
                LogEvent.logWarn(this.getClass().getSimpleName(), "persistdata",
                        "Result with id: " + id + " created while validating");
            }
            if (isResultAnalysisFinalized(resultUpdate, analysisUpdateList)) {
                try {
                    testNotificationService.createAndSendNotificationsToConfiguredSources(
                            NotificationNature.RESULT_VALIDATION, resultUpdate);
                } catch (RuntimeException e) {
                    LogEvent.logError(e);
                }
            }
        }

        checkIfSamplesFinished(resultItemList, sampleUpdateList);

        // update finished samples
        for (Sample sample : sampleUpdateList) {
            sampleService.update(sample);
        }

        // create or update notes
        for (Note note : noteUpdateList) {
            if (note != null) {
                if (note.getId() == null) {
                    if (!noteService.duplicateNoteExists(note)) {
                        noteService.insert(note);
                    }
                } else {
                    noteService.update(note);
                }
            }
        }

        for (IResultUpdate updater : updaters) {
            updater.transactionalUpdate(resultSaveService);
        }
    }

    private boolean isResultAnalysisFinalized(Result result, List<Analysis> analysisUpdateList) {
        String analysisId = result.getAnalysis().getId();
        for (Analysis analysis : analysisUpdateList) {
            if (analysis.getId().equals(analysisId)) {
                return analysis.getStatusId()
                        .equals(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
            }
        }
        return false;
    }

    private void checkIfSamplesFinished(List<AnalysisItem> resultItemList, List<Sample> sampleUpdateList) {
        String currentSampleId = "";
        boolean sampleFinished = true;
        List<Integer> sampleFinishedStatus = getSampleFinishedStatuses();

        for (AnalysisItem analysisItem : resultItemList) {
            String analysisSampleId = sampleService.getSampleByAccessionNumber(analysisItem.getAccessionNumber())
                    .getId();
            if (!analysisSampleId.equals(currentSampleId)) {

                currentSampleId = analysisSampleId;

                List<Analysis> analysisList = analysisService.getAnalysesBySampleId(currentSampleId);

                for (Analysis analysis : analysisList) {
                    if (!sampleFinishedStatus.contains(Integer.parseInt(analysis.getStatusId()))) {
                        sampleFinished = false;
                        break;
                    }
                }

                if (sampleFinished) {
                    Sample sample = sampleService.get(currentSampleId);
                    sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));
                    sampleUpdateList.add(sample);
                }

                sampleFinished = true;
            }
        }
    }

    /**
     * Returns the release-status id (Finalized). Cached on first call.
     */
    private String getFinalizedStatusId() {
        return SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized);
    }

    /**
     * Transitions QC analyses (BLANK / DUPLICATE / CONTROL) on any sample whose
     * client analyses are being released to Finalized status, so the sample as a
     * whole can roll up to "Finished". {@code released_date} is intentionally left
     * null — QC results aren't released to patients.
     */
    private void transitionQcAnalysesInBatch(List<Analysis> analysisUpdateList, String sysUserId) {
        if (analysisUpdateList == null || analysisUpdateList.isEmpty()) {
            return;
        }
        String finalizedStatusId = getFinalizedStatusId();
        java.util.Set<String> samplesBeingReleased = new java.util.HashSet<>();
        for (Analysis a : analysisUpdateList) {
            if (finalizedStatusId.equals(a.getStatusId()) && a.getSampleItem() != null
                    && a.getSampleItem().getSample() != null) {
                samplesBeingReleased.add(a.getSampleItem().getSample().getId());
            }
        }
        if (samplesBeingReleased.isEmpty()) {
            return;
        }

        java.util.Set<String> alreadyInUpdateList = new java.util.HashSet<>();
        for (Analysis a : analysisUpdateList) {
            alreadyInUpdateList.add(a.getId());
        }

        for (String sampleId : samplesBeingReleased) {
            List<Analysis> batch = analysisService.getAnalysesBySampleId(sampleId);
            if (batch == null) {
                continue;
            }
            for (Analysis sibling : batch) {
                if (sibling.getSampleItem() == null || sibling.getSampleItem().getId() == null) {
                    continue;
                }
                if (alreadyInUpdateList.contains(sibling.getId())) {
                    continue;
                }
                if (finalizedStatusId.equals(sibling.getStatusId())) {
                    continue;
                }
                Integer sampleItemIdInt;
                try {
                    sampleItemIdInt = Integer.valueOf(sibling.getSampleItem().getId());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (sampleItemQcProfileDAO.findBySampleItemId(sampleItemIdInt).isEmpty()) {
                    continue;
                }
                sibling.setStatusId(finalizedStatusId);
                sibling.setSysUserId(sysUserId);
                // released_date intentionally left null — these are QC results, not
                // released patient results.
                analysisUpdateList.add(sibling);
            }
        }
    }

    /**
     * Throws {@link QcAcknowledgmentRequiredException} if any analysis being
     * transitioned to Finalized belongs to a batch (sample) that has failed QC
     * results without a corresponding acknowledgment row.
     */
    private void enforceQcAcknowledgment(List<Analysis> analysisUpdateList) {
        if (analysisUpdateList == null || analysisUpdateList.isEmpty()) {
            return;
        }

        String finalizedStatusId = getFinalizedStatusId();
        // Distinct sample ids whose batch we need to gate-check.
        java.util.Set<String> sampleIdsToCheck = new java.util.HashSet<>();
        for (Analysis a : analysisUpdateList) {
            if (finalizedStatusId.equals(a.getStatusId()) && a.getSampleItem() != null
                    && a.getSampleItem().getSample() != null) {
                sampleIdsToCheck.add(a.getSampleItem().getSample().getId());
            }
        }
        if (sampleIdsToCheck.isEmpty()) {
            return;
        }

        java.util.List<String> unacknowledged = new java.util.ArrayList<>();
        for (String sampleId : sampleIdsToCheck) {
            List<Analysis> batch = analysisService.getAnalysesBySampleId(sampleId);
            if (batch == null) {
                continue;
            }
            for (Analysis sibling : batch) {
                List<Result> siblingResults = resultService.getResultsByAnalysis(sibling);
                if (siblingResults == null) {
                    continue;
                }
                boolean failed = false;
                for (Result r : siblingResults) {
                    if (r.getQcEvaluation() == QcEvaluation.FAIL) {
                        failed = true;
                        break;
                    }
                }
                if (!failed) {
                    continue;
                }
                Integer analysisIdInt;
                try {
                    analysisIdInt = Integer.valueOf(sibling.getId());
                } catch (NumberFormatException e) {
                    continue;
                }
                if (qcAcknowledgmentDAO.findByAnalysisId(analysisIdInt).isEmpty()) {
                    unacknowledged.add(sibling.getId());
                }
            }
        }

        if (!unacknowledged.isEmpty()) {
            throw new QcAcknowledgmentRequiredException(
                    "QC failures must be acknowledged before releasing results. Unacknowledged analyses: "
                            + unacknowledged);
        }
    }

    @Override
    @Transactional
    public void persistQcAcknowledgment(ValidationQcAcknowledgment acknowledgment) {
        qcAcknowledgmentDAO.insert(acknowledgment);
        // Mirror to the generic history audit trail (S-08 FR-04 requires acknowledgment
        // + justification to be in the audit trail; the dedicated table holds the full
        // record, this row makes it discoverable from the standard audit-trail UI).
        if (acknowledgment.getAcknowledgedBy() != null) {
            auditTrailService.saveNewHistory(acknowledgment, acknowledgment.getAcknowledgedBy().toString(),
                    "validation_qc_acknowledgment");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ValidationQcAcknowledgment> getQcAcknowledgmentsByAnalysisId(Integer analysisId) {
        return qcAcknowledgmentDAO.findByAnalysisId(analysisId);
    }

    private List<Integer> getSampleFinishedStatuses() {
        ArrayList<Integer> sampleFinishedStatus = new ArrayList<>();
        sampleFinishedStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized)));
        sampleFinishedStatus.add(
                Integer.parseInt(SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Canceled)));
        sampleFinishedStatus.add(Integer.parseInt(
                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.NonConforming_depricated)));
        return sampleFinishedStatus;
    }
}
