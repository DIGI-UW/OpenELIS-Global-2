package org.openelisglobal.testcalculated.service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.notifications.service.HeaderNotificationService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultlimit.service.ResultLimitService;
import org.openelisglobal.resultlimits.valueholder.ResultLimit;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.systemuser.service.UserService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testcalculated.egfr.CkdEpi2021;
import org.openelisglobal.testcalculated.egfr.EgfrDecision;
import org.openelisglobal.testcalculated.egfr.EgfrDecision.Cause;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.openelisglobal.testterminology.valueholder.TestTerminologyMapping;
import org.openelisglobal.userrole.service.UserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * eGFR from creatinine with CKD-EPI 2021, for labs that order an eGFR test.
 *
 * <p>
 * Tests are recognised by LOINC, from the test's LOINC field or an active LOINC
 * terminology mapping: creatinine is 2160-0 or 14682-9, with a unit of measure
 * of mg/dL or µmol/L; eGFR is 98979-8. Nothing happens on an order without an
 * eGFR test, so the feature is off until a lab adds one to its catalog.
 *
 * <p>
 * This is not a configurable "calculated value" (Calculation): those see a
 * patient's age on the day of the save rather than at collection, and have no
 * operand for sex, which the equation needs.
 *
 * <p>
 * {@link Analysis#getResultCalculated()} on the eGFR records whether the value
 * stored was written here. An eGFR saved by hand or by an analyzer clears it,
 * and from then on that eGFR is never overwritten.
 *
 * <p>
 * A correction (a stored eGFR replaced or cleared, or a validated eGFR the
 * equation no longer agrees with) is put in the header notifications of the
 * users who can validate the eGFR's lab unit.
 */
@Service
public class EgfrCalculationServiceImpl implements EgfrCalculationService {

    static final String NOTE_SUBJECT = "eGFR " + CkdEpi2021.METHOD;
    private static final String LOINC = "LOINC";

    private final AnalysisService analysisService;
    private final ResultService resultService;
    private final TestResultService testResultService;
    private final ResultLimitService resultLimitService;
    private final SampleHumanService sampleHumanService;
    private final NoteService noteService;
    private final IStatusService statusService;
    private final TestTerminologyMappingService terminologyMappingService;
    private final HeaderNotificationService headerNotificationService;
    private final UserRoleService userRoleService;
    private final UserService userService;

    public EgfrCalculationServiceImpl(AnalysisService analysisService, ResultService resultService,
            TestResultService testResultService, ResultLimitService resultLimitService,
            SampleHumanService sampleHumanService, NoteService noteService, IStatusService statusService,
            TestTerminologyMappingService terminologyMappingService,
            HeaderNotificationService headerNotificationService, UserRoleService userRoleService,
            UserService userService) {
        this.analysisService = analysisService;
        this.resultService = resultService;
        this.testResultService = testResultService;
        this.resultLimitService = resultLimitService;
        this.sampleHumanService = sampleHumanService;
        this.noteService = noteService;
        this.statusService = statusService;
        this.terminologyMappingService = terminologyMappingService;
        this.headerNotificationService = headerNotificationService;
        this.userRoleService = userRoleService;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void updateEgfrForSavedResults(Collection<Result> savedResults, String sysUserId) {
        if (savedResults == null || savedResults.isEmpty()) {
            return;
        }
        Map<String, Set<String>> loincsByTest = new HashMap<>();
        Set<String> savedAnalysisIds = new HashSet<>();
        for (Result result : savedResults) {
            Analysis analysis = result == null ? null : result.getAnalysis();
            if (analysis != null && analysis.getId() != null) {
                savedAnalysisIds.add(analysis.getId());
            }
        }

        for (Result result : savedResults) {
            if (result != null && isEgfr(result.getAnalysis(), loincsByTest) && hasValue(result.getValue())) {
                markEnteredElsewhere(result.getAnalysis(), sysUserId);
            }
        }

        for (Result result : savedResults) {
            Analysis creatinine = result == null ? null : result.getAnalysis();
            if (creatinine == null || !isCreatinine(creatinine.getTest(), loincsByTest)) {
                continue;
            }
            try {
                updateEgfrFor(result, creatinine, savedAnalysisIds, loincsByTest, sysUserId);
            } catch (RuntimeException e) {
                // the eGFR is a convenience; it must not stop the creatinine being saved
                LogEvent.logError(this.getClass().getSimpleName(), "updateEgfrForSavedResults",
                        "eGFR for creatinine analysis " + creatinine.getId() + " failed: " + e.getMessage());
            }
        }
    }

    private void updateEgfrFor(Result creatinineResult, Analysis creatinine, Set<String> savedAnalysisIds,
            Map<String, Set<String>> loincsByTest, String sysUserId) {
        if (creatinine.getSampleItem() == null || creatinine.getSampleItem().getSample() == null) {
            return;
        }
        Sample sample = creatinine.getSampleItem().getSample();
        Analysis egfr = findOrderedEgfr(sample, creatinine, loincsByTest);
        if (egfr == null || savedAnalysisIds.contains(egfr.getId())) {
            // no eGFR ordered, or it was entered in this same save
            return;
        }
        Patient patient = sampleHumanService.getPatientForSample(sample);
        apply(Cause.CREATININE_SAVED, egfr, creatinine, creatinineResult, sample, patient, sysUserId);
    }

    @Override
    @Transactional
    public void updateEgfrForPatientChange(Patient patient, String previousGender, Timestamp previousBirthDate,
            String sysUserId) {
        if (patient == null || patient.getId() == null) {
            return;
        }
        if (Objects.equals(trimmed(previousGender), trimmed(patient.getGender()))
                && Objects.equals(toLocalDate(previousBirthDate), toLocalDate(patient.getBirthDate()))) {
            return;
        }
        Map<String, Set<String>> loincsByTest = new HashMap<>();
        for (Sample sample : sampleHumanService.getSamplesForPatient(patient.getId())) {
            try {
                updateEgfrsOnSample(sample, patient, loincsByTest, sysUserId);
            } catch (RuntimeException e) {
                // the eGFR is a convenience; it must not stop the patient being saved
                LogEvent.logError(this.getClass().getSimpleName(), "updateEgfrForPatientChange",
                        "eGFR for sample " + sample.getId() + " failed: " + e.getMessage());
            }
        }
    }

    /**
     * Every eGFR on the sample that was calculated here, or is still empty, from
     * the creatinine on the same order.
     */
    private void updateEgfrsOnSample(Sample sample, Patient patient, Map<String, Set<String>> loincsByTest,
            String sysUserId) {
        List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
        for (Analysis egfr : analyses) {
            if (!isEgfr(egfr, loincsByTest) || isCanceledOrRejected(egfr)) {
                continue;
            }
            Result egfrResult = firstResult(egfr);
            if (egfrResult != null && hasValue(egfrResult.getValue())
                    && !Boolean.TRUE.equals(egfr.getResultCalculated())) {
                // entered by hand or by an analyzer
                continue;
            }
            Analysis creatinine = findCreatinine(analyses, egfr, loincsByTest);
            Result creatinineResult = creatinine == null ? null : firstResult(creatinine);
            if (creatinineResult == null) {
                continue;
            }
            apply(Cause.PATIENT_CHANGED, egfr, creatinine, creatinineResult, sample, patient, sysUserId);
        }
    }

    private void apply(Cause cause, Analysis egfr, Analysis creatinine, Result creatinineResult, Sample sample,
            Patient patient, String sysUserId) {
        Result egfrResult = firstResult(egfr);
        String unit = creatinine.getTest().getUnitOfMeasure() == null ? null
                : creatinine.getTest().getUnitOfMeasure().getUnitOfMeasureName();
        CkdEpi2021.Outcome outcome = patient == null ? CkdEpi2021.Outcome.skipped(CkdEpi2021.SkipReason.NO_PATIENT)
                : CkdEpi2021.calculate(creatinineResult.getValue(), unit, patient.getGender(),
                        toLocalDate(patient.getBirthDate()), collectionDay(sample));
        EgfrDecision decision = EgfrDecision.decide(cause, outcome, creatinineResult.getValue(), unit,
                egfrResult == null ? null : egfrResult.getValue(), Boolean.TRUE.equals(egfr.getResultCalculated()),
                statusService.matches(egfr.getStatusId(), AnalysisStatus.Finalized));

        switch (decision.getAction()) {
        case WRITE:
            write(egfr, egfrResult, decision.getValue(), patient, sysUserId);
            break;
        case CLEAR:
            clear(egfr, egfrResult, sysUserId);
            break;
        default:
            break;
        }
        if (decision.getNote() != null) {
            Note note = noteService.createSavableNote(egfr, NoteType.INTERNAL, decision.getNote(), NOTE_SUBJECT,
                    sysUserId);
            if (!noteService.duplicateNoteExists(note)) {
                noteService.insert(note);
            }
        }
        if (decision.isCorrection()) {
            notifyValidators(cause, egfr, sample, decision);
        }
    }

    /**
     * Header notification for the users who can validate the eGFR's lab unit, as
     * for STAT results.
     */
    private void notifyValidators(Cause cause, Analysis egfr, Sample sample, EgfrDecision decision) {
        try {
            String message = correctionMessage(cause, sample.getAccessionNumber(), decision);
            List<Analysis> egfrOnly = List.of(egfr);
            for (String userId : userRoleService.getUserIdsForRole(Constants.ROLE_VALIDATION)) {
                if (!userService.filterAnalysesByLabUnitRoles(userId, egfrOnly, Constants.ROLE_VALIDATION).isEmpty()) {
                    headerNotificationService.notifyUser(userId, message);
                }
            }
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "notifyValidators",
                    "eGFR notification for analysis " + egfr.getId() + " failed: " + e.getMessage());
        }
    }

    static String correctionMessage(Cause cause, String accessionNumber, EgfrDecision decision) {
        String causeText = MessageUtil.getMessage(cause == Cause.PATIENT_CHANGED ? "notification.egfr.cause.patient"
                : "notification.egfr.cause.creatinine");
        switch (decision.getAction()) {
        case WRITE:
            return MessageUtil.getMessage("notification.egfr.recalculated",
                    new Object[] { accessionNumber, causeText, decision.getPreviousValue(), decision.getValue() });
        case CLEAR:
            return MessageUtil.getMessage("notification.egfr.cleared",
                    new Object[] { accessionNumber, causeText, decision.getPreviousValue() });
        default:
            return MessageUtil.getMessage("notification.egfr.validated",
                    new Object[] { accessionNumber, causeText, decision.getPreviousValue() });
        }
    }

    private void write(Analysis egfr, Result existing, String value, Patient patient, String sysUserId) {
        Result result = existing == null ? new Result() : existing;
        if (result.getTestResult() == null) {
            List<TestResult> testResults = testResultService.getActiveTestResultsByTest(egfr.getTest().getId());
            if (!testResults.isEmpty()) {
                result.setTestResult(testResults.get(0));
            }
        }
        result.setAnalysis(egfr);
        result.setResultType("N");
        result.setValue(value);
        result.setSignificantDigits(0);
        result.setSysUserId(sysUserId);
        ResultLimit limit = resultLimitService.getResultLimitForResult(egfr, result, patient);
        if (limit != null) {
            result.setMinNormal(limit.getLowNormal());
            result.setMaxNormal(limit.getHighNormal());
        }
        if (result.getId() == null) {
            resultService.insert(result);
        } else {
            resultService.update(result);
        }

        if (statusService.matches(egfr.getStatusId(), AnalysisStatus.NotStarted)) {
            egfr.setStatusId(statusService.getStatusID(AnalysisStatus.TechnicalAcceptance));
        }
        if (egfr.getStartedDate() == null) {
            egfr.setStartedDate(new Timestamp(System.currentTimeMillis()));
        }
        egfr.setEnteredDate(new Timestamp(System.currentTimeMillis()));
        egfr.setResultCalculated(true);
        egfr.setSysUserId(sysUserId);
        analysisService.update(egfr);
    }

    private void clear(Analysis egfr, Result result, String sysUserId) {
        result.setValue("");
        result.setSysUserId(sysUserId);
        resultService.update(result);
        if (statusService.matches(egfr.getStatusId(), AnalysisStatus.TechnicalAcceptance)) {
            egfr.setStatusId(statusService.getStatusID(AnalysisStatus.NotStarted));
        }
        egfr.setResultCalculated(false);
        egfr.setSysUserId(sysUserId);
        analysisService.update(egfr);
    }

    /** An eGFR saved by hand or by an analyzer is not ours to overwrite later. */
    private void markEnteredElsewhere(Analysis egfr, String sysUserId) {
        if (Boolean.TRUE.equals(egfr.getResultCalculated())) {
            egfr.setResultCalculated(false);
            egfr.setSysUserId(sysUserId);
            analysisService.update(egfr);
        }
    }

    /** The eGFR on the creatinine's order: on the same specimen if there is one. */
    private Analysis findOrderedEgfr(Sample sample, Analysis creatinine, Map<String, Set<String>> loincsByTest) {
        Analysis onOtherItem = null;
        for (Analysis analysis : analysisService.getAnalysesBySampleId(sample.getId())) {
            if (!isEgfr(analysis, loincsByTest) || isCanceledOrRejected(analysis)) {
                continue;
            }
            if (analysis.getSampleItem() != null && creatinine.getSampleItem() != null
                    && analysis.getSampleItem().getId().equals(creatinine.getSampleItem().getId())) {
                return analysis;
            }
            if (onOtherItem == null) {
                onOtherItem = analysis;
            }
        }
        return onOtherItem;
    }

    /** The creatinine for an eGFR: on the same specimen if there is one. */
    private Analysis findCreatinine(List<Analysis> analyses, Analysis egfr, Map<String, Set<String>> loincsByTest) {
        Analysis onOtherItem = null;
        for (Analysis analysis : analyses) {
            if (!isCreatinine(analysis.getTest(), loincsByTest) || isCanceledOrRejected(analysis)) {
                continue;
            }
            if (analysis.getSampleItem() != null && egfr.getSampleItem() != null
                    && analysis.getSampleItem().getId().equals(egfr.getSampleItem().getId())) {
                return analysis;
            }
            if (onOtherItem == null) {
                onOtherItem = analysis;
            }
        }
        return onOtherItem;
    }

    private boolean isCanceledOrRejected(Analysis analysis) {
        return statusService.matches(analysis.getStatusId(), AnalysisStatus.Canceled)
                || statusService.matches(analysis.getStatusId(), AnalysisStatus.SampleRejected);
    }

    private Result firstResult(Analysis analysis) {
        List<Result> results = resultService.getResultsByAnalysis(analysis);
        return results == null || results.isEmpty() ? null : results.get(0);
    }

    private boolean isCreatinine(Test test, Map<String, Set<String>> loincsByTest) {
        Set<String> loincs = loincs(test, loincsByTest);
        return loincs.contains(CREATININE_MASS_LOINC) || loincs.contains(CREATININE_MOLES_LOINC);
    }

    private boolean isEgfr(Analysis analysis, Map<String, Set<String>> loincsByTest) {
        return analysis != null && loincs(analysis.getTest(), loincsByTest).contains(EGFR_CKD_EPI_2021_LOINC);
    }

    /** The test's LOINC codes: its LOINC field plus its active LOINC mappings. */
    private Set<String> loincs(Test test, Map<String, Set<String>> loincsByTest) {
        if (test == null || test.getId() == null) {
            return Set.of();
        }
        return loincsByTest.computeIfAbsent(test.getId(), id -> {
            Set<String> codes = new HashSet<>();
            if (hasValue(test.getLoinc())) {
                codes.add(test.getLoinc().trim());
            }
            for (TestTerminologyMapping mapping : terminologyMappingService.getActiveByTestId(id)) {
                if (LOINC.equalsIgnoreCase(mapping.getSource()) && hasValue(mapping.getCode())) {
                    codes.add(mapping.getCode().trim());
                }
            }
            return codes;
        });
    }

    private static LocalDate collectionDay(Sample sample) {
        Timestamp collected = sample.getCollectionDate();
        return toLocalDate(collected != null ? collected : sample.getReceivedTimestamp());
    }

    private static LocalDate toLocalDate(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toLocalDate();
    }

    private static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }
}
