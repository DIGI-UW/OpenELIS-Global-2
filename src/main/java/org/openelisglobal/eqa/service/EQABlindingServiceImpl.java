package org.openelisglobal.eqa.service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.eqa.dao.EQAPanelDAO;
import org.openelisglobal.eqa.dao.EQAPanelSampleDAO;
import org.openelisglobal.eqa.dao.EQAParticipantResultDAO;
import org.openelisglobal.eqa.dao.EQARoundDAO;
import org.openelisglobal.eqa.dao.EQASchemeAnalystDAO;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.openelisglobal.eqa.valueholder.EQAPanel;
import org.openelisglobal.eqa.valueholder.EQAPanelSample;
import org.openelisglobal.eqa.valueholder.EQAPanelStatus;
import org.openelisglobal.eqa.valueholder.EQAParticipantResult;
import org.openelisglobal.eqa.valueholder.EQAPerformanceStatus;
import org.openelisglobal.eqa.valueholder.EQARound;
import org.openelisglobal.eqa.valueholder.EQASchemeAnalyst;
import org.openelisglobal.eqa.valueholder.EQASchemeType;
import org.openelisglobal.eqa.valueholder.EQASubmissionStatus;
import org.openelisglobal.eqa.valueholder.EQAUnblindMethod;
import org.openelisglobal.eqa.valueholder.SampleEQA;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.samplehuman.valueholder.SampleHuman;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-612 (FR-V2.4) — the in-house blinding flows. Order creation deliberately
 * reuses the same service layer the standard order wizard bottoms out in
 * (SampleService.insertDataWithAccessionNumber + item/analysis services) — the
 * wizard facade itself is request-bound and unusable from a scheduler.
 */
@Service
@Transactional
public class EQABlindingServiceImpl implements EQABlindingService {

    private static final String IN_HOUSE_PROVIDER = "In-house";
    /** sample.accession_number is VARCHAR(25); blind_code allows 50. */
    private static final int ACCESSION_NUMBER_MAX = 25;

    @Autowired
    private EQAPanelService panelService;
    @Autowired
    private EQAPanelDAO panelDAO;
    @Autowired
    private EQAPanelSampleDAO panelSampleDAO;
    @Autowired
    private EQARoundDAO roundDAO;
    @Autowired
    private EQASchemeAnalystDAO schemeAnalystDAO;
    @Autowired
    private EQAParticipantResultService participantResultService;
    @Autowired
    private EQAParticipantResultDAO participantResultDAO;
    @Autowired
    private ResultService resultService;
    @Autowired
    private EQALabProgramEnrollmentService labEnrollmentService;
    @Autowired
    private SampleService sampleService;
    @Autowired
    private SampleItemService sampleItemService;
    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private SampleEQAService sampleEQAService;
    @Autowired
    private PatientService patientService;
    @Autowired
    private PersonService personService;
    @Autowired
    private TestService testService;
    @Autowired
    private TypeOfSampleService typeOfSampleService;
    @Autowired
    private IStatusService statusService;

    @Override
    public Map<String, Object> sealAndDistribute(Long panelId, List<BlindOrderSpec> specs, String sysUserId) {
        EQAPanel panel = panelService.get(panelId);
        if (panel.getScheme() == null || panel.getScheme().getSchemeType() != EQASchemeType.IN_HOUSE) {
            throw new IllegalArgumentException("Blinded distribution is for in-house schemes only");
        }
        if (panel.getCycle() == null) {
            throw new IllegalArgumentException("The panel needs a cycle before it can be distributed as orders");
        }

        List<EQAPanelSample> samples = panelSampleDAO.getAllMatchingOrdered("panel.id", panelId, "sampleCode", false);
        requireFullCoverage(samples, specs);
        requirePrepEvidence(panel, samples);
        requireUsableBlindCodes(samples);

        // Existing seal validation: legal edge, targets present, unblind date set.
        panel = panelService.seal(panelId, sysUserId);

        EQARound round = findOrCreateRound(panel, sysUserId);
        EQALabProgramEnrollment enrollment = findOrCreateSelfEnrollment(panel, sysUserId);
        List<Long> roster = analystRoster(panel);

        Patient blindPatient = createBlindPatient(sysUserId);
        Map<Long, EQAPanelSample> samplesById = new LinkedHashMap<>();
        for (EQAPanelSample sample : samples) {
            samplesById.put(sample.getId(), sample);
        }

        List<String> accessions = new ArrayList<>();
        int rosterCursor = 0;
        for (BlindOrderSpec spec : specs) {
            EQAPanelSample panelSample = samplesById.get(spec.panelSampleId());
            Long analystId = spec.analystId();
            if (analystId == null && !roster.isEmpty()) {
                analystId = roster.get(rosterCursor++ % roster.size());
            }
            accessions.add(createBlindOrder(panel, round, enrollment, panelSample, spec.testId(), analystId,
                    blindPatient, sysUserId));
        }

        panel = panelService.distribute(panelId, sysUserId);

        Map<String, Object> dto = new LinkedHashMap<>(panelService.toPanelDto(panel));
        dto.put("orderAccessionNumbers", accessions);
        return dto;
    }

    @Override
    public EQAPanel unblindAndScore(Long panelId, String sysUserId, EQAUnblindMethod method) {
        // The DISTRIBUTED → UNBLINDED edge is the idempotency guard, taken under
        // a row lock so a manual and a scheduled unblind cannot both pass the
        // edge check against the same prior state (the cycle machine hit exactly
        // this race and fixed it the same way).
        EQAPanel panel = panelService.unblindForUpdate(panelId, sysUserId, method);

        // One result per aliquot: a panel may carry several samples for the same
        // analyte (FR-V2.4-02 Mode A splits a pool into N), so results are keyed
        // by panel sample and each is scored against its own sealed target.
        Map<Long, EQAPanelSample> samplesById = new LinkedHashMap<>();
        for (EQAPanelSample sample : panelSampleDAO.getAllMatchingOrdered("panel.id", panelId, "sampleCode", false)) {
            samplesById.put(sample.getId(), sample);
        }

        for (EQAParticipantResult result : participantResultService.getAllMatching("cycle.id",
                panel.getCycle().getId())) {
            EQAPanelSample target = samplesById.get(result.getPanelSampleId());
            if (target == null) {
                continue; // belongs to another panel in this cycle, or to external PT
            }
            resolveResult(result, target, sysUserId);
        }

        panel.setStatus(EQAPanelStatus.SCORED);
        panel.setSysUserId(sysUserId);
        return panelDAO.update(panel);
    }

    // ---- seal helpers ----

    /**
     * AC-V2.4-13: the prep invariants are enforced here, not only in the wizard, so
     * an API caller cannot distribute a panel the bench never signed off. Failing
     * homogeneity QC is allowed, but only with the written justification the
     * standard asks for.
     */
    private void requirePrepEvidence(EQAPanel panel, List<EQAPanelSample> samples) {
        int produced = panel.getAliquotsProduced() == null ? 0 : panel.getAliquotsProduced();
        if (produced < samples.size()) {
            throw new IllegalArgumentException(
                    "Panel has " + samples.size() + " samples but records only " + produced + " aliquots produced");
        }
        if (!Boolean.TRUE.equals(panel.getHomogeneityQcPassed())
                && GenericValidator.isBlankOrNull(panel.getHomogeneityQcNotes())) {
            throw new IllegalArgumentException(
                    "Homogeneity QC has not passed, so distributing requires a written justification");
        }
    }

    /**
     * The blind code becomes the order's accession number, which is globally unique
     * and narrower than the blind_code column. Both facts have to be checked before
     * any order is written, or a late collision aborts a half-created distribution.
     */
    private void requireUsableBlindCodes(List<EQAPanelSample> samples) {
        Set<String> seen = new HashSet<>();
        for (EQAPanelSample sample : samples) {
            String code = sample.getBlindCode();
            if (GenericValidator.isBlankOrNull(code)) {
                throw new IllegalArgumentException("Panel sample " + sample.getSampleCode() + " has no blind code");
            }
            if (code.length() > ACCESSION_NUMBER_MAX) {
                throw new IllegalArgumentException("Blind code '" + code + "' exceeds the " + ACCESSION_NUMBER_MAX
                        + " characters an accession number allows");
            }
            if (!seen.add(code)) {
                throw new IllegalArgumentException("Blind code '" + code + "' is used twice in this panel");
            }
            if (sampleService.getSampleByAccessionNumber(code) != null) {
                throw new IllegalArgumentException("Blind code '" + code + "' is already an order accession number");
            }
        }
    }

    private void requireFullCoverage(List<EQAPanelSample> samples, List<BlindOrderSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new IllegalArgumentException("Distribution needs one order spec (testId) per panel sample");
        }
        Set<Long> expected = new HashSet<>();
        for (EQAPanelSample sample : samples) {
            expected.add(sample.getId());
        }
        Set<Long> provided = new HashSet<>();
        for (BlindOrderSpec spec : specs) {
            if (spec.panelSampleId() == null || GenericValidator.isBlankOrNull(spec.testId())) {
                throw new IllegalArgumentException("Every order spec needs a panelSampleId and a testId");
            }
            if (!provided.add(spec.panelSampleId())) {
                throw new IllegalArgumentException("Panel sample " + spec.panelSampleId() + " appears twice");
            }
            if (testService.get(spec.testId()) == null) {
                throw new IllegalArgumentException("Unknown test " + spec.testId());
            }
        }
        if (!provided.equals(expected)) {
            throw new IllegalArgumentException("Order specs must cover every panel sample exactly once");
        }
    }

    private EQARound findOrCreateRound(EQAPanel panel, String sysUserId) {
        List<EQARound> rounds = roundDAO.getAllMatchingOrdered("cycle.id", panel.getCycle().getId(), "roundNumber",
                false);
        if (!rounds.isEmpty()) {
            return rounds.get(0);
        }
        EQARound round = new EQARound();
        round.setFhirUuid(UUID.randomUUID());
        round.setCycle(panel.getCycle());
        round.setRoundNumber(1);
        round.setDistributionDate(DateUtil.getNowAsTimestamp());
        if (panel.getUnblindDate() != null) {
            round.setSubmissionDeadline(new Timestamp(panel.getUnblindDate().getTime()));
        }
        round.setSysUserId(sysUserId);
        round.setId(roundDAO.insert(round));
        return round;
    }

    /**
     * eqa_participant_result.lab_enrollment_id is NOT NULL by schema; for an
     * in-house scheme the lab participates in its own program, so a self-enrollment
     * row (provider "In-house", named after the scheme) is created once and reused.
     */
    private EQALabProgramEnrollment findOrCreateSelfEnrollment(EQAPanel panel, String sysUserId) {
        String programName = panel.getScheme().getName();
        for (EQALabProgramEnrollment enrollment : labEnrollmentService.getAllMatching("programName", programName)) {
            if (IN_HOUSE_PROVIDER.equals(enrollment.getProvider())) {
                return enrollment;
            }
        }
        EQALabProgramEnrollment enrollment = new EQALabProgramEnrollment();
        enrollment.setProgramName(programName);
        enrollment.setProvider(IN_HOUSE_PROVIDER);
        enrollment.setDescription("Self-enrollment for in-house blinded PT");
        enrollment.setIsActive(true);
        enrollment.setCreatedDate(new java.sql.Date(System.currentTimeMillis()));
        enrollment.setSysUserId(sysUserId);
        enrollment.setId(labEnrollmentService.insert(enrollment));
        return enrollment;
    }

    private List<Long> analystRoster(EQAPanel panel) {
        List<Long> roster = new ArrayList<>();
        for (EQASchemeAnalyst analyst : schemeAnalystDAO.getAllMatchingOrdered("scheme.id", panel.getScheme().getId(),
                "id", false)) {
            roster.add(analyst.getSystemUserId());
        }
        return roster;
    }

    /**
     * The blind patient carries no demographics on purpose — matching the V1 EQA
     * order convention where PT orders surface as a nameless patient.
     */
    private Patient createBlindPatient(String sysUserId) {
        Person person = new Person();
        person.setSysUserId(sysUserId);
        personService.insert(person);

        Patient patient = new Patient();
        patient.setPerson(person);
        patient.setSysUserId(sysUserId);
        patientService.insert(patient);
        return patient;
    }

    private String createBlindOrder(EQAPanel panel, EQARound round, EQALabProgramEnrollment enrollment,
            EQAPanelSample panelSample, String testId, Long analystId, Patient patient, String sysUserId) {
        Test test = testService.get(testId);

        // FR-V2.4-04/-15: the blind code IS the accession number, so Workplan
        // and result entry display it with no EQA-specific code path.
        Sample sample = new Sample();
        sample.setAccessionNumber(panelSample.getBlindCode());
        sample.setDomain("H");
        sample.setEnteredDate(DateUtil.getNowAsSqlDate());
        sample.setReceivedTimestamp(DateUtil.getNowAsTimestamp());
        sample.setStatusId(statusService.getStatusID(OrderStatus.Entered));
        // Result entry dereferences priority unconditionally, so a blinded order
        // cannot be left with the column null.
        sample.setPriority(OrderPriority.ROUTINE);
        sample.setFhirUuid(UUID.randomUUID());
        sample.setSysUserId(sysUserId);
        sampleService.insertDataWithAccessionNumber(sample);

        SampleHuman sampleHuman = new SampleHuman();
        sampleHuman.setSampleId(sample.getId());
        sampleHuman.setPatientId(patient.getId());
        sampleHuman.setSysUserId(sysUserId);
        sampleHumanService.insert(sampleHuman);

        SampleItem sampleItem = new SampleItem();
        sampleItem.setSample(sample);
        sampleItem.setSortOrder("1");
        sampleItem.setStatusId(statusService.getStatusID(SampleStatus.Entered));
        List<TypeOfSample> types = typeOfSampleService.getTypeOfSampleForTest(test.getId());
        if (types != null && !types.isEmpty()) {
            sampleItem.setTypeOfSample(types.get(0));
        }
        sampleItem.setFhirUuid(UUID.randomUUID());
        sampleItem.setSysUserId(sysUserId);
        sampleItemService.insert(sampleItem);

        Analysis analysis = new Analysis();
        analysis.setTest(test);
        analysis.setIsReportable(test.getIsReportable());
        analysis.setAnalysisType("MANUAL");
        analysis.setSampleItem(sampleItem);
        analysis.setRevision(ConfigurationProperties.getInstance().getPropertyValue("analysis.default.revision"));
        analysis.setStartedDate(DateUtil.getNowAsTimestamp());
        analysis.setStatusId(statusService.getStatusID(AnalysisStatus.NotStarted));
        analysis.setTestSection(test.getTestSection());
        analysis.setFhirUuid(UUID.randomUUID());
        analysis.setSysUserId(sysUserId);
        analysisService.insert(analysis);

        SampleEQA sampleEQA = new SampleEQA();
        sampleEQA.setSampleId(Long.parseLong(sample.getId()));
        sampleEQA.setIsEqaSample(true);
        sampleEQA.setEqaEnrollmentId(enrollment.getId());
        sampleEQA.setEqaProviderSampleId(panelSample.getBlindCode());
        if (panel.getUnblindDate() != null) {
            sampleEQA.setEqaDeadline(new Timestamp(panel.getUnblindDate().getTime()));
        }
        sampleEQA.setCycleId(panel.getCycle().getId());
        sampleEQA.setRoundId(round.getId());
        sampleEQA.setSysUserId(sysUserId);
        sampleEQAService.insert(sampleEQA);

        EQAParticipantResult draft = new EQAParticipantResult();
        draft.setFhirUuid(UUID.randomUUID());
        draft.setCycle(panel.getCycle());
        draft.setRound(round);
        draft.setLabEnrollmentId(enrollment.getId());
        draft.setAnalyteId(panelSample.getAnalyteId());
        draft.setPanelSampleId(panelSample.getId());
        draft.setAnalysisId(Long.parseLong(analysis.getId()));
        draft.setAssignedAnalystId(analystId);
        draft.setSysUserId(sysUserId);
        participantResultService.saveDraft(draft);

        return panelSample.getBlindCode();
    }

    // ---- unblind helpers ----

    private void resolveResult(EQAParticipantResult result, EQAPanelSample target, String sysUserId) {
        if (result.getSubmissionStatus() == EQASubmissionStatus.SCORED
                || result.getSubmissionStatus() == EQASubmissionStatus.MISSED_DEADLINE) {
            return; // already resolved — keeps re-runs from double-scoring
        }

        // The analyst runs a blinded panel through standard result entry, so the
        // reported value lives on the analysis, not on this row. Read it here
        // rather than expecting something to have copied it across: nothing
        // does, and treating a blank column as "no result" marks every analyst
        // who did the work as having missed the deadline.
        String reported = reportedValueOf(result);
        if (GenericValidator.isBlankOrNull(reported)) {
            participantResultService.markMissedDeadline(result.getId(), sysUserId);
            return;
        }
        if (!reported.equals(result.getResultValue())) {
            result.setResultValue(reported);
            result.setSysUserId(sysUserId);
            participantResultDAO.update(result);
        }

        if (result.getSubmissionStatus() != EQASubmissionStatus.SUBMITTED) {
            promoteToSubmitted(result, sysUserId);
        }
        score(result, target, sysUserId);
    }

    /**
     * The value the analyst actually entered: the row's own column when a client
     * supplied one, otherwise the linked analysis's result. Dictionary-backed
     * results store a dictionary id in result.value, so the printable form is what
     * gets compared against a target a supervisor typed by hand.
     */
    private String reportedValueOf(EQAParticipantResult result) {
        if (!GenericValidator.isBlankOrNull(result.getResultValue())) {
            return result.getResultValue().trim();
        }
        if (result.getAnalysisId() == null) {
            return null;
        }
        Analysis analysis = analysisService.get(String.valueOf(result.getAnalysisId()));
        if (analysis == null) {
            return null;
        }
        for (Result pipelineResult : resultService.getResultsByAnalysis(analysis)) {
            String value = EqaReportedValue.of(resultService, pipelineResult);
            if (!GenericValidator.isBlankOrNull(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private void promoteToSubmitted(EQAParticipantResult result, String sysUserId) {
        if (result.getSubmissionStatus() == EQASubmissionStatus.DRAFT) {
            participantResultService.transitionStatus(result.getId(), EQASubmissionStatus.VALIDATED_PARTIAL, sysUserId);
        }
        participantResultService.transitionStatus(result.getId(), EQASubmissionStatus.SUBMITTED, sysUserId);
    }

    /**
     * In-house has no Z by construction (FR-V2.4-07), so the verdict is the whole
     * score. Routing an unacceptable one to the Follow-Up Queue (FR-V2.4-08) is the
     * tiered adapter's job, fired by recordScore — this method deliberately does
     * not enqueue, or the failure would be registered twice.
     */
    private void score(EQAParticipantResult result, EQAPanelSample target, String sysUserId) {
        participantResultService.recordScore(result.getId(), verdictFor(target, result.getResultValue()), null,
                sysUserId);
    }

    /**
     * FR-V2.4-07 / AC-V2.4-07/-08: numeric when an acceptance range is sealed
     * (inside the closed range = acceptable), categorical exact match otherwise. A
     * non-numeric report against a numeric target is a mismatch, not an error.
     */
    private EQAPerformanceStatus verdictFor(EQAPanelSample target, String reported) {
        String value = reported == null ? "" : reported.trim();
        if (target.getAcceptanceRangeLow() != null || target.getAcceptanceRangeHigh() != null) {
            BigDecimal numeric;
            try {
                numeric = new BigDecimal(value);
            } catch (NumberFormatException e) {
                return EQAPerformanceStatus.UNACCEPTABLE;
            }
            if (target.getAcceptanceRangeLow() != null && numeric.compareTo(target.getAcceptanceRangeLow()) < 0) {
                return EQAPerformanceStatus.UNACCEPTABLE;
            }
            if (target.getAcceptanceRangeHigh() != null && numeric.compareTo(target.getAcceptanceRangeHigh()) > 0) {
                return EQAPerformanceStatus.UNACCEPTABLE;
            }
            return EQAPerformanceStatus.ACCEPTABLE;
        }
        String targetValue = target.getTargetValue() == null ? "" : target.getTargetValue().trim();
        // A quantitative target sealed without a range still has to compare as a
        // number, or "100.0" fails against a target of "100".
        BigDecimal targetNumber = parseOrNull(targetValue);
        BigDecimal reportedNumber = parseOrNull(value);
        if (targetNumber != null && reportedNumber != null) {
            return targetNumber.compareTo(reportedNumber) == 0 ? EQAPerformanceStatus.ACCEPTABLE
                    : EQAPerformanceStatus.UNACCEPTABLE;
        }
        return targetValue.equalsIgnoreCase(value) ? EQAPerformanceStatus.ACCEPTABLE
                : EQAPerformanceStatus.UNACCEPTABLE;
    }

    private static BigDecimal parseOrNull(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
