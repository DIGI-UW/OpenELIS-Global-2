package org.openelisglobal.microbiology.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.microbiology.dao.MicroAntibioticDAO;
import org.openelisglobal.microbiology.dao.MicroAstReadingDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseAnalysisDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroOrganismDAO;
import org.openelisglobal.microbiology.valueholder.MicroAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
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
    private final MicroOrganismDAO organismDAO;
    private final MicroAntibioticDAO antibioticDAO;
    private final AnalysisService analysisService;
    private final TestAnalyteService testAnalyteService;
    private final ResultService resultService;
    private final IStatusService statusService;

    public MicroReportProjectionServiceImpl(MicroCaseDAO caseDAO, MicroCaseAnalysisDAO caseAnalysisDAO,
            MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO, MicroAstReadingDAO readingDAO,
            MicroOrganismDAO organismDAO, MicroAntibioticDAO antibioticDAO, AnalysisService analysisService,
            TestAnalyteService testAnalyteService, ResultService resultService, IStatusService statusService) {
        this.caseDAO = caseDAO;
        this.caseAnalysisDAO = caseAnalysisDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.readingDAO = readingDAO;
        this.organismDAO = organismDAO;
        this.antibioticDAO = antibioticDAO;
        this.analysisService = analysisService;
        this.testAnalyteService = testAnalyteService;
        this.resultService = resultService;
        this.statusService = statusService;
    }

    @Override
    @Transactional
    public MicroReportProjectionResult releasePreliminary(String caseId, String performedBy) {
        ProjectionInput input = projectionInput(caseId);
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
    @Transactional(readOnly = true)
    public MicroReportProjectionResult preview(String caseId) {
        ProjectionInput input = projectionInput(caseId);
        List<String> projectedResultIds = input.links().stream().map(MicroCaseAnalysis::getProjectedResultId)
                .filter(this::hasText).toList();
        return new MicroReportProjectionResult(input.content(), input.mappingConfigured(), projectedResultIds);
    }

    private ProjectionInput projectionInput(String caseId) {
        MicroCaseServiceImpl.requireText(caseId, "caseId");
        MicroCase microCase = caseDAO.get(caseId).orElseThrow(() -> new IllegalArgumentException("Case not found"));
        String content = buildContent(microCase);
        List<MicroCaseAnalysis> links = caseAnalysisDAO.getByCaseId(caseId);
        boolean mappingConfigured = !links.isEmpty()
                && links.stream().allMatch(link -> hasText(link.getReportableTestAnalyteId()));
        return new ProjectionInput(content, links, mappingConfigured);
    }

    private MicroReportProjectionResult persist(ProjectionInput input, String performedBy, boolean finalizeAnalyses) {
        ensureResultValueFits(input.content());
        List<String> projectedResultIds = new ArrayList<>();
        for (MicroCaseAnalysis link : input.links()) {
            Analysis analysis = analysisService.get(link.getAnalysisId());
            TestAnalyte testAnalyte = testAnalyteService.get(link.getReportableTestAnalyteId());
            validateMapping(analysis, testAnalyte);
            Result result = existingOrNewResult(link, analysis);
            result.setAnalysis(analysis);
            result.setAnalyte(testAnalyte.getAnalyte());
            result.setIsReportable("Y");
            result.setResultType(ResultType.ALPHA.getCharacterValue());
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

    private void finalizeAnalysis(Analysis analysis, String performedBy) {
        analysis.setStatusId(statusService.getStatusID(AnalysisStatus.Finalized));
        analysis.setReleasedDate(new Timestamp(System.currentTimeMillis()));
        analysis.setSysUserId(performedBy);
        analysisService.update(analysis);
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
            for (MicroAstRun run : astRunDAO.getByIsolateId(isolate.getId())) {
                if (!MicroAstRunStatus.REVIEWED.name().equals(run.getStatus())) {
                    continue;
                }
                for (MicroAstReading reading : readingDAO.getByRunId(run.getId())) {
                    readings.add(antibioticName(reading.getAntibioticId()) + " " + interpretation(reading));
                }
            }
            String value = isolate.getIsolateLabel() + ": " + identification;
            if (readings.length() > 0) {
                value += " (" + readings + ")";
            }
            isolates.add(value);
        }
        return isolates.toString();
    }

    private String identificationFor(MicroIsolate isolate) {
        if (MicroIsolateIdentificationStatus.PENDING.name().equals(isolate.getIdentificationStatus())) {
            return null;
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
