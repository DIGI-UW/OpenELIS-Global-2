package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseAnalysis;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateIdentificationStatus;
import org.openelisglobal.microbiology.valueholder.MicroOrganism;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.testanalyte.service.TestAnalyteService;
import org.openelisglobal.testanalyte.valueholder.TestAnalyte;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroReportProjectionServiceImpl implements MicroReportProjectionService {

    private static final int RESULT_VALUE_LIMIT = 200;

    private final MicroCaseDAO caseDAO;
    private final MicroCaseAnalysisDAO caseAnalysisDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroAstReadingDAO readingDAO;
    private final MicroAstRunAntibioticDAO runAntibioticDAO;
    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final AnalysisService analysisService;
    private final TestAnalyteService testAnalyteService;
    private final TestResultService testResultService;
    private final ResultService resultService;
    private final IStatusService statusService;

    public MicroReportProjectionServiceImpl(MicroCaseDAO caseDAO, MicroCaseAnalysisDAO caseAnalysisDAO,
            MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO, MicroAstReadingDAO readingDAO,
            MicroAstRunAntibioticDAO runAntibioticDAO, MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO,
            AnalysisService analysisService, TestAnalyteService testAnalyteService, TestResultService testResultService,
            ResultService resultService, IStatusService statusService) {
        this.caseDAO = caseDAO;
        this.caseAnalysisDAO = caseAnalysisDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.readingDAO = readingDAO;
        this.runAntibioticDAO = runAntibioticDAO;
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.analysisService = analysisService;
        this.testAnalyteService = testAnalyteService;
        this.testResultService = testResultService;
        this.resultService = resultService;
        this.statusService = statusService;
    }

    @Override
    @Transactional
    public MicroReportProjectionResult releasePreliminary(String caseId, String performedBy) {
        MicroCase microCase = getCase(caseId);
        if (MicroCaseStage.NO_GROWTH_READY.name().equals(microCase.getStage())) {
            throw new IllegalStateException("FINAL_NEGATIVE_RELEASE_REQUIRED");
        }
        ProjectionInput input = projectionInput(microCase);
        requireContent(input.content());
        if (!input.mappingConfigured()) {
            return new MicroReportProjectionResult(input.content(), false, List.of());
        }
        return persist(input, performedBy, false);
    }

    @Override
    @Transactional
    public MicroReportProjectionResult releaseFinal(String caseId, String performedBy) {
        ProjectionInput input = projectionInput(caseId);
        requireContent(input.content());
        if (!input.mappingConfigured()) {
            throw new IllegalStateException("REPORT_MAPPING_REQUIRED");
        }
        return persist(input, performedBy, true);
    }

    @Override
    @Transactional
    public MicroReportProjectionResult releaseAmended(String caseId, String performedBy) {
        ProjectionInput input = projectionInput(caseId);
        requireContent(input.content());
        if (!input.mappingConfigured()) {
            throw new IllegalStateException("REPORT_MAPPING_REQUIRED");
        }
        for (MicroCaseAnalysis link : input.links()) {
            Analysis original = analysisService.get(link.getAnalysisId());
            Analysis revised = analysisService.buildAnalysis(original.getTest(), original.getSampleItem());
            revised.setRevision(nextRevision(original.getRevision()));
            revised.setMethod(original.getMethod());
            revised.setPanel(original.getPanel());
            revised.setAnalysisType(original.getAnalysisType());
            revised.setIsReportable(original.getIsReportable());
            revised.setSysUserId(performedBy);
            String revisedId = analysisService.insert(revised);
            if (!hasText(revised.getId())) {
                revised.setId(revisedId);
            }
            link.setAnalysisId(revised.getId());
            link.setProjectedResultId(null);
        }
        return persist(input, performedBy, true);
    }

    @Override
    @Transactional(readOnly = true)
    public MicroReportProjectionResult preview(String caseId) {
        MicroCase microCase = getCase(caseId);
        if (MicroCaseStage.FINAL_RELEASED.name().equals(microCase.getStage())) {
            return projectionResult(releasedProjectionInput(microCase));
        }
        ProjectionInput input;
        try {
            input = projectionInput(microCase);
        } catch (MicroAstConflictException conflict) {
            if (!"REPORTABLE_AST_RUN_REQUIRED".equals(conflict.getMessage())) {
                throw conflict;
            }
            input = projectionInput(microCase, "");
        }
        return projectionResult(input);
    }

    private MicroReportProjectionResult projectionResult(ProjectionInput input) {
        List<String> projectedResultIds = input.links().stream().map(MicroCaseAnalysis::getProjectedResultId)
                .filter(this::hasText).toList();
        return new MicroReportProjectionResult(input.content(), input.mappingConfigured(), projectedResultIds);
    }

    private ProjectionInput projectionInput(String caseId) {
        return projectionInput(getCase(caseId));
    }

    private MicroCase getCase(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        return caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
    }

    private ProjectionInput projectionInput(MicroCase microCase) {
        return projectionInput(microCase, buildContent(microCase));
    }

    private ProjectionInput projectionInput(MicroCase microCase, String content) {
        List<MicroCaseAnalysis> links = caseAnalysisDAO.getByCaseId(microCase.getId());
        boolean mappingConfigured = !links.isEmpty() && links.stream().allMatch(this::hasReportConfiguration);
        return new ProjectionInput(content, links, mappingConfigured);
    }

    private ProjectionInput releasedProjectionInput(MicroCase microCase) {
        List<MicroCaseAnalysis> links = caseAnalysisDAO.getByCaseId(microCase.getId());
        List<String> releasedValues = links.stream().map(MicroCaseAnalysis::getProjectedResultId).filter(this::hasText)
                .map(resultService::getResultById).filter(result -> result != null && hasText(result.getValue()))
                .map(result -> result.getValue().trim()).distinct().toList();
        if (releasedValues.size() > 1) {
            throw new IllegalStateException("FINAL_REPORT_BASELINE_AMBIGUOUS");
        }
        String content = releasedValues.isEmpty() ? "" : releasedValues.get(0);
        boolean mappingConfigured = !links.isEmpty() && links.stream().allMatch(this::hasReportConfiguration);
        return new ProjectionInput(content, links, mappingConfigured);
    }

    private MicroReportProjectionResult persist(ProjectionInput input, String performedBy, boolean finalizeAnalyses) {
        ensureResultValueFits(input.content());
        List<String> projectedResultIds = new ArrayList<>();
        for (MicroCaseAnalysis link : input.links()) {
            Analysis analysis = analysisService.get(link.getAnalysisId());
            TestAnalyte testAnalyte = testAnalyteService.get(link.getReportableTestAnalyteId());
            validateMapping(analysis, testAnalyte);
            TestResult reportTestResult = reportTestResult(analysis);
            if (reportTestResult == null) {
                throw new IllegalStateException("REPORT_MAPPING_INVALID");
            }
            Result result = existingOrNewResult(link, analysis);
            result.setAnalysis(analysis);
            result.setAnalyte(testAnalyte.getAnalyte());
            result.setIsReportable("Y");
            result.setResultType(ResultType.REMARK.getCharacterValue());
            result.setTestResult(reportTestResult);
            result.setSortOrder("0");
            result.setValue(input.content());
            result.setSysUserId(performedBy);
            String resultId = saveResult(result);
            if (!resultId.equals(link.getProjectedResultId())) {
                link.setProjectedResultId(resultId);
                caseAnalysisDAO.update(link);
            }
            projectedResultIds.add(resultId);
            if (finalizeAnalyses) {
                finalizeAnalysis(analysis, performedBy);
            }
        }
        return new MicroReportProjectionResult(input.content(), true, projectedResultIds);
    }

    private Result existingOrNewResult(MicroCaseAnalysis link, Analysis analysis) {
        if (!hasText(link.getProjectedResultId())) {
            return new Result();
        }
        Result result = resultService.getResultById(link.getProjectedResultId());
        if (result == null) {
            throw new IllegalStateException("Projected patient-report result is missing");
        }
        if (result.getAnalysis() != null && !analysis.getId().equals(result.getAnalysis().getId())) {
            throw new IllegalStateException("Projected patient-report result belongs to another analysis");
        }
        return result;
    }

    private String saveResult(Result result) {
        if (hasText(result.getId())) {
            resultService.update(result);
            return result.getId();
        }
        String resultId = resultService.insert(result);
        if (!hasText(result.getId())) {
            result.setId(resultId);
        }
        return result.getId();
    }

    private void validateMapping(Analysis analysis, TestAnalyte testAnalyte) {
        if (testAnalyte == null || testAnalyte.getAnalyte() == null) {
            throw new IllegalStateException("REPORT_MAPPING_INVALID");
        }
        if (!"Y".equalsIgnoreCase(testAnalyte.getIsReportable())) {
            throw new IllegalStateException("REPORT_MAPPING_NOT_REPORTABLE");
        }
        if (analysis == null || analysis.getTest() == null || testAnalyte.getTest() == null
                || !analysis.getTest().getId().equals(testAnalyte.getTest().getId())) {
            throw new IllegalStateException("REPORT_MAPPING_TEST_MISMATCH");
        }
    }

    private boolean hasReportConfiguration(MicroCaseAnalysis link) {
        if (!hasText(link.getReportableTestAnalyteId())) {
            return false;
        }
        return reportTestResult(analysisService.get(link.getAnalysisId())) != null;
    }

    private TestResult reportTestResult(Analysis analysis) {
        if (analysis == null || analysis.getTest() == null) {
            return null;
        }
        return testResultService.getAllActiveTestResultsPerTest(analysis.getTest()).stream()
                .filter(testResult -> ResultType.REMARK.matches(testResult.getTestResultType())).findFirst()
                .orElse(null);
    }

    private void finalizeAnalysis(Analysis analysis, String performedBy) {
        analysis.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
        analysis.setReleasedDate(new Timestamp(System.currentTimeMillis()));
        analysis.setSysUserId(performedBy);
        analysisService.update(analysis);
    }

    private String nextRevision(String revision) {
        if (!hasText(revision)) {
            return "1";
        }
        try {
            return Integer.toString(Integer.parseInt(revision) + 1);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("ANALYSIS_REVISION_INVALID", e);
        }
    }

    private String buildContent(MicroCase microCase) {
        if (MicroCaseStage.NO_GROWTH_READY.name().equals(microCase.getStage())) {
            return "No growth";
        }
        StringJoiner isolates = new StringJoiner("; ");
        for (MicroIsolate isolate : isolateDAO.getByCaseId(microCase.getId())) {
            String identification = identificationFor(isolate);
            if (identification == null) {
                continue;
            }
            StringJoiner readings = new StringJoiner(", ");
            for (MicroAstRun run : reportableRuns(isolate.getId())) {
                for (MicroAstReading reading : currentOrderedReadings(run.getId())) {
                    readings.add(antibioticName(reading.getAntibioticId()) + " " + interpretation(reading));
                }
            }
            String value = isolate.getIsolateLabel() + ": " + identification;
            if (readings.length() > 0) {
                value += "; " + readings;
            }
            isolates.add(value);
        }
        return isolates.toString();
    }

    private List<MicroAstReading> currentOrderedReadings(String runId) {
        Map<String, MicroAstReading> currentByAntibiotic = new HashMap<>();
        for (MicroAstReading reading : readingDAO.getByRunId(runId)) {
            MicroAstReading current = currentByAntibiotic.get(reading.getAntibioticId());
            if (current == null || isLater(reading, current)) {
                currentByAntibiotic.put(reading.getAntibioticId(), reading);
            }
        }
        List<MicroAstRunAntibiotic> ordered = runAntibioticDAO.getByRunId(runId);
        if (ordered.isEmpty()) {
            throw new MicroAstConflictException("AST_ORDERED_RESULTS_INCOMPLETE");
        }
        List<MicroAstReading> currentOrdered = new ArrayList<>();
        for (MicroAstRunAntibiotic orderedAntibiotic : ordered) {
            MicroAstReading reading = currentByAntibiotic.get(orderedAntibiotic.getAntibioticId());
            if (reading == null) {
                throw new MicroAstConflictException("AST_ORDERED_RESULTS_INCOMPLETE");
            }
            currentOrdered.add(reading);
        }
        return currentOrdered;
    }

    private boolean isLater(MicroAstReading candidate, MicroAstReading current) {
        Timestamp candidateTime = candidate.getCreatedAt();
        Timestamp currentTime = current.getCreatedAt();
        if (candidateTime != null && currentTime != null && !candidateTime.equals(currentTime)) {
            return candidateTime.after(currentTime);
        }
        if (candidateTime != null && currentTime == null) {
            return true;
        }
        if (candidateTime == null && currentTime != null) {
            return false;
        }
        String candidateId = candidate.getId() == null ? "" : candidate.getId();
        String currentId = current.getId() == null ? "" : current.getId();
        return candidateId.compareTo(currentId) > 0;
    }

    private List<MicroAstRun> reportableRuns(String isolateId) {
        List<MicroAstRun> reviewed = astRunDAO.getByIsolateId(isolateId).stream()
                .filter(run -> MicroAstRunStatus.REVIEWED.name().equals(run.getStatus())).toList();
        if (reviewed.size() <= 1) {
            return reviewed;
        }
        List<MicroAstRun> selected = reviewed.stream().filter(MicroAstRun::isReportable).toList();
        if (selected.size() != 1) {
            throw new MicroAstConflictException("REPORTABLE_AST_RUN_REQUIRED");
        }
        return selected;
    }

    private String identificationFor(MicroIsolate isolate) {
        if (MicroIsolateIdentificationStatus.PENDING.name().equals(isolate.getIdentificationStatus())) {
            return null;
        }
        if (!MicroIsolateIdentificationStatus.CONFIRMED.name().equals(isolate.getIdentificationStatus())) {
            if (!hasText(isolate.getGramStain())) {
                return null;
            }
            String workup = "Gram stain: " + isolate.getGramStain().trim();
            if (hasText(isolate.getColonyMorphology())) {
                workup += "; Colony morphology: " + isolate.getColonyMorphology().trim();
            }
            return workup;
        }
        if (hasText(isolate.getOrganismId())) {
            MicroOrganism organism = organismDAO.get(isolate.getOrganismId()).orElse(null);
            if (organism != null && hasText(organism.getDisplayName())) {
                return organism.getDisplayName();
            }
        }
        return hasText(isolate.getPreliminaryOrganismText()) ? isolate.getPreliminaryOrganismText() : null;
    }

    private String antibioticName(String antibioticId) {
        MicroAntibiotic antibiotic = antibioticDAO.get(antibioticId).orElse(null);
        return antibiotic != null && hasText(antibiotic.getDisplayName()) ? antibiotic.getDisplayName() : antibioticId;
    }

    private String interpretation(MicroAstReading reading) {
        String value = hasText(reading.getOverrideInterpretation()) ? reading.getOverrideInterpretation()
                : reading.getInterpretation();
        if (MicroAstInterpretation.SUSCEPTIBLE.name().equals(value)) {
            return "S";
        }
        if (MicroAstInterpretation.INTERMEDIATE.name().equals(value)) {
            return "I";
        }
        if (MicroAstInterpretation.RESISTANT.name().equals(value)) {
            return "R";
        }
        return "NB";
    }

    private void requireContent(String content) {
        if (!hasText(content)) {
            throw new IllegalStateException("REPORTABLE_CONTENT_REQUIRED");
        }
    }

    private void ensureResultValueFits(String content) {
        if (content.length() > RESULT_VALUE_LIMIT) {
            throw new IllegalStateException("REPORT_CONTENT_TOO_LONG");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private record ProjectionInput(String content, List<MicroCaseAnalysis> links, boolean mappingConfigured) {
    }
}
