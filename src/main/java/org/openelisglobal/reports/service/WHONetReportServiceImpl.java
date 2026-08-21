package org.openelisglobal.reports.service;

import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.StatusService.OrderStatus;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.microbiology.dao.MicroWhonetExportRunDAO;
import org.openelisglobal.microbiology.form.MicroWhonetExportQueryForm;
import org.openelisglobal.microbiology.form.MicroWhonetFilterOptionsForm;
import org.openelisglobal.microbiology.form.MicroWhonetPreviewForm;
import org.openelisglobal.microbiology.service.MicroWhonetDataset;
import org.openelisglobal.microbiology.service.MicroWhonetDatasetService;
import org.openelisglobal.microbiology.service.MicroWhonetExportBlockedException;
import org.openelisglobal.microbiology.valueholder.MicroWhonetExportRun;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder;
import org.openelisglobal.reports.action.implementation.reportBeans.WHONETCSVRoutineColumnBuilder.WHONetRow;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testreflex.action.util.TestReflexUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WHONetReportServiceImpl implements WHONetReportService {

    private final MicroWhonetDatasetService microWhonetDatasetService;
    private final MicroWhonetExportRunDAO microWhonetExportRunDAO;

    public WHONetReportServiceImpl(MicroWhonetDatasetService microWhonetDatasetService,
            MicroWhonetExportRunDAO microWhonetExportRunDAO) {
        this.microWhonetDatasetService = microWhonetDatasetService;
        this.microWhonetExportRunDAO = microWhonetExportRunDAO;
    }

    @Autowired
    private AnalysisService analysisService;
    @Autowired
    private ResultService resultService;
    @Autowired
    private SampleHumanService sampleHumanService;
    @Autowired
    private TestService testService;
    @Autowired
    private IStatusService statusService;

    private List<String> ANALYSIS_STATUS_IDS;
    private List<String> SAMPLE_STATUS_IDS;

    @PostConstruct
    private void initialize() {
        ANALYSIS_STATUS_IDS = Arrays.asList(statusService.getStatusID(AnalysisStatus.SampleRejected),
                statusService.getStatusID(AnalysisStatus.NotStarted),
                statusService.getStatusID(AnalysisStatus.Canceled),
                statusService.getStatusID(AnalysisStatus.TechnicalAcceptance),
                statusService.getStatusID(AnalysisStatus.TechnicalRejected),
                statusService.getStatusID(AnalysisStatus.BiologistRejected),
                statusService.getStatusID(AnalysisStatus.NonConforming_depricated),
                statusService.getStatusID(AnalysisStatus.Finalized)).stream().collect(Collectors.toList());
        SAMPLE_STATUS_IDS = Arrays
                .asList(statusService.getStatusID(OrderStatus.Entered), statusService.getStatusID(OrderStatus.Started),
                        statusService.getStatusID(OrderStatus.Finished),
                        statusService.getStatusID(OrderStatus.NonConforming_depricated))
                .stream().collect(Collectors.toList());
    }

    @Override
    public List<SampleItem> getAntimicrobialEntries(Date lowDate, Date highDate) {
        List<Test> tests = testService.getAllMatching("antimicrobialResistance", Boolean.TRUE);
        List<String> testIds = tests.stream().map(e -> e.getId()).collect(Collectors.toList());

        Map<String, SampleItem> sampleItems = new HashMap<>();
        List<Analysis> analysises = analysisService.getAllAnalysisByTestsAndStatusAndCompletedDateRange(testIds,
                ANALYSIS_STATUS_IDS, SAMPLE_STATUS_IDS, lowDate, highDate);

        analysises.stream().forEach(e -> sampleItems.putIfAbsent(e.getSampleItem().getId(), e.getSampleItem()));

        return new ArrayList<SampleItem>(sampleItems.values());
    }

    @Override
    public List<WHONetRow> getWHONetRows(Date lowDate, Date highDate) {
        List<SampleItem> sampleItems = getAntimicrobialEntries(lowDate, highDate);
        return createRowsFromSampleItems(sampleItems);
    }

    private List<WHONetRow> createRowsFromSampleItems(List<SampleItem> sampleItems) {
        List<WHONetRow> rows = new ArrayList<>();

        List<String> triggerTestIds = testService.getTriggeringAntimicrobialResistanceTests().stream()
                .map(e -> e.getId()).collect(Collectors.toList());

        for (SampleItem sampleItem : sampleItems) {
            Sample sample = sampleItem.getSample();
            Patient patient = sampleHumanService.getPatientForSample(sample);
            List<Analysis> analysises = analysisService.getAnalysesBySampleItem(sampleItem);

            // for every analysis in the sampleItem, find any that are a triggering
            // antimicrobial resistance analysis
            for (Analysis potentialTriggerAnalysis : analysises) {
                if (triggerTestIds.contains(potentialTriggerAnalysis.getTest().getId())) {
                    Analysis triggerAnalysis = potentialTriggerAnalysis;
                    // find the exact result that triggers further analysis
                    List<Result> results = resultService.getResultsByAnalysis(triggerAnalysis);
                    for (Result triggerResult : results) {
                        TestReflexUtil testReflexUtil = new TestReflexUtil();

                        // find the analysis that was triggered by the trigger test
                        for (Analysis potentialReflexAnalysis : analysises) {
                            if (testReflexUtil.isTestTriggeredByResult(potentialReflexAnalysis.getTest(),
                                    triggerResult)) {
                                Analysis reflexAnalysis = potentialReflexAnalysis;
                                // get the results from the the reflex test
                                List<Result> reflexResults = resultService.getResultsByAnalysis(reflexAnalysis);
                                if (reflexResults.size() == 0) {
                                    // if there's no results for the second test, we still need the info of the
                                    // trigger test and reflex test name
                                    rows.add(new WHONetRow(patient.getNationalId(), patient.getPerson().getFirstName(),
                                            patient.getPerson().getLastName(), patient.getGender(),
                                            patient.getBirthDateForDisplay(), sample.getEnteredDateForDisplay(),
                                            sample.getAccessionNumber(),
                                            DateUtil.convertTimestampToStringDate(sampleItem.getCollectionDate()),
                                            sampleItem.getTypeOfSample().getDescription(),
                                            reflexAnalysis.getTest().getName(),
                                            resultService.getSimpleResultValue(triggerResult), "",
                                            reflexAnalysis.getMethod() == null ? ""
                                                    : reflexAnalysis.getMethod().getLocalizedValue(),
                                            sample.getGpsLatitude() != null ? sample.getGpsLatitude().toString() : "",
                                            sample.getGpsLongitude() != null ? sample.getGpsLongitude().toString()
                                                    : ""));
                                } else {
                                    // else add the info of both tests/results
                                    for (Result reflexResult : reflexResults) {
                                        rows.add(new WHONetRow(patient.getNationalId(),
                                                patient.getPerson().getFirstName(), patient.getPerson().getLastName(),
                                                patient.getGender(), patient.getBirthDateForDisplay(),
                                                sample.getEnteredDateForDisplay(), sample.getAccessionNumber(),
                                                DateUtil.convertTimestampToStringDate(sampleItem.getCollectionDate()),
                                                sampleItem.getTypeOfSample().getDescription(),
                                                reflexAnalysis.getTest().getName(),
                                                resultService.getSimpleResultValue(triggerResult),
                                                resultService.getSimpleResultValue(reflexResult),
                                                reflexAnalysis.getMethod() == null ? ""
                                                        : reflexAnalysis.getMethod().getLocalizedValue(),
                                                sample.getGpsLatitude() != null ? sample.getGpsLatitude().toString()
                                                        : "",
                                                sample.getGpsLongitude() != null ? sample.getGpsLongitude().toString()
                                                        : ""));
                                    }
                                }
                            }

                        }
                    }
                }
            }

        }

        return rows;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroWhonetPreviewForm previewMicrobiologyExport(MicroWhonetExportQueryForm query) {
        return microWhonetDatasetService.compile(query).getPreview();
    }

    @Override
    @Transactional(readOnly = true)
    public MicroWhonetFilterOptionsForm getMicrobiologyExportFilterOptions(MicroWhonetExportQueryForm query) {
        return microWhonetDatasetService.getFilterOptions(query);
    }

    @Override
    @Transactional
    public MicroWhonetExportResult generateMicrobiologyExport(MicroWhonetExportQueryForm query,
            String authenticatedUserId) {
        if (authenticatedUserId == null || authenticatedUserId.trim().isEmpty()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        MicroWhonetDataset dataset = microWhonetDatasetService.compile(query);
        MicroWhonetPreviewForm preview = dataset.getPreview();
        if (!preview.canGenerate || dataset.getRows().isEmpty()) {
            throw new MicroWhonetExportBlockedException("No valid WHONET rows remain after validation");
        }

        byte[] content = WHONETCSVRoutineColumnBuilder.renderRows(dataset.getRows());
        String fileName = "WHONET_" + preview.from + "_to_" + preview.to + ".csv";
        MicroWhonetExportRun run = new MicroWhonetExportRun();
        run.setFromDate(Date.valueOf(preview.from));
        run.setToDate(Date.valueOf(preview.to));
        run.setSignificancePolicy(preview.significance);
        run.setDedupPolicy(preview.dedup);
        run.setPopulationSelection(dataset.getPopulationSelection());
        run.setCaseCount(preview.totalCases);
        run.setIsolateCount(preview.exportableIsolates);
        run.setRowCount(preview.exportedRows);
        run.setExcludedRowCount(preview.excludedRows);
        run.setFileName(fileName);
        run.setContentSha256(sha256(content));
        run.setGeneratedAt(new Timestamp(System.currentTimeMillis()));
        run.setGeneratedBy(authenticatedUserId);
        microWhonetExportRunDAO.insert(run);
        return new MicroWhonetExportResult(fileName, content);
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

}
