package org.openelisglobal.resultrun.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerMappingDefinition;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerMapping;
import org.openelisglobal.analyzerimport.dao.AnalyzerDeliveryReceiptDAO;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.common.services.IResultSaveService;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.AnalysisStatus;
import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
import org.openelisglobal.inventory.service.InventoryLotService;
import org.openelisglobal.inventory.valueholder.InventoryLot;
import org.openelisglobal.result.service.ResultService;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.resultrun.dao.ResultRunDAO;
import org.openelisglobal.resultrun.form.ResultRunChip;
import org.openelisglobal.resultrun.form.ResultRunChipCounts;
import org.openelisglobal.resultrun.form.ResultRunHeader;
import org.openelisglobal.resultrun.form.ResultRunListResponse;
import org.openelisglobal.resultrun.form.ResultRunRow;
import org.openelisglobal.resultrun.form.ResultRunRowCategory;
import org.openelisglobal.resultrun.form.ResultRunRowsPage;
import org.openelisglobal.resultrun.form.ResultRunSelection;
import org.openelisglobal.resultrun.form.ResultRunSelectionSummary;
import org.openelisglobal.resultrun.form.ResultRunSummary;
import org.openelisglobal.resultrun.form.RunHoldCause;
import org.openelisglobal.resultrun.form.RunTestVerdict;
import org.openelisglobal.resultrun.form.RunViewer;
import org.openelisglobal.resultrun.form.WaitingRun;
import org.openelisglobal.resultrun.service.ResultRunRowClassifier.ClassifiedRow;
import org.openelisglobal.resultrun.valueholder.ResultRun;
import org.openelisglobal.resultrun.valueholder.ResultRunSource;
import org.openelisglobal.resultrun.valueholder.ResultRunState;
import org.openelisglobal.test.beanItems.TestResultItem;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.test.valueholder.TestSection;
import org.openelisglobal.testresultcomponent.service.TestResultComponentService;
import org.openelisglobal.testresultcomponent.valueholder.TestResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResultRunServiceImpl extends BaseObjectServiceImpl<ResultRun, Long> implements ResultRunService {

    static final int DEFAULT_PAGE_SIZE = 50;
    private static final List<Integer> PAGE_SIZES = List.of(25, 50, 100);

    @Autowired
    private ResultRunDAO baseObjectDAO;
    @Autowired
    private AnalyzerService analyzerService;
    @Autowired
    private TestService testService;
    @Autowired
    private TestSectionService testSectionService;
    @Autowired
    private AnalyzerDeliveryReceiptDAO receiptDAO;
    @Autowired
    private ResultService resultService;
    @Autowired
    private TestResultComponentService testResultComponentService;
    @Lazy
    @Autowired
    private IStatusService statusService;
    @Lazy
    @Autowired
    private InventoryLotService inventoryLotService;
    @Lazy
    @Autowired
    private AnalysisService analysisService;

    /** A workplan run's row names the analysis it is: "A" and its id. */
    static final String WORKPLAN_ROW_PREFIX = "A";
    @Autowired
    private ResultRunRowClassifier rowClassifier;
    @Lazy
    @Autowired
    private AnalyzerResultsDAO analyzerResultsDAO;
    @Lazy
    @Autowired
    private RunQcService runQcService;
    @Lazy
    @Autowired
    private RunExceptionService runExceptionService;

    public ResultRunServiceImpl() {
        super(ResultRun.class);
    }

    @Override
    protected ResultRunDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional
    public Optional<ResultRun> openAnalyzerRun(Analyzer analyzer, String connectionId, String messageId,
            List<AnalyzerResults> stagedRows) {
        List<AnalyzerResults> runRows = stagedRows.stream()
                .filter(row -> !AnalyzerResults.IMPORT_ISSUE_SETUP_CAPTURE.equals(row.getImportIssueReason())).toList();
        if (runRows.isEmpty()) {
            return Optional.empty();
        }
        Timestamp now = Timestamp.from(Instant.now());
        Optional<ResultRun> existing = baseObjectDAO.findAnalyzerRun(connectionId, messageId);
        if (existing.isPresent()) {
            ResultRun run = existing.get();
            run.setRowCount(run.getRowCount() + runRows.size());
            run.setLastActivityAt(now);
            if (!run.getState().isActive() && run.getState() != ResultRunState.SUPERSEDED) {
                run.setState(ResultRunState.OPEN);
                run.setCompletedAt(null);
            }
            update(run);
            runRows.forEach(row -> row.setRunId(run.getId()));
            return Optional.of(run);
        }
        ResultRun run = new ResultRun();
        run.setSource(ResultRunSource.ANALYZER);
        run.setState(ResultRunState.OPEN);
        run.setAnalyzerId(analyzer.getId());
        List<String> units = analyzer.getTestUnitIds();
        run.setTestSectionId(units != null && units.size() == 1 ? units.get(0) : null);
        run.setSourceConnectionId(connectionId);
        run.setSourceMessageId(messageId);
        run.setOperatorName(runRows.stream().map(AnalyzerResults::getInstrumentOperator).filter(StringUtils::isNotBlank)
                .findFirst().orElse(null));
        run.setRowCount(runRows.size());
        run.setOpenedAt(now);
        run.setLastActivityAt(now);
        Long id = insert(run);
        run.setId(id);
        runRows.forEach(row -> row.setRunId(id));
        return Optional.of(run);
    }

    @Override
    @Transactional
    public void attachReceipt(Long runId, String receiptId) {
        ResultRun run = get(runId);
        run.setDeliveryReceiptId(receiptId);
        update(run);
    }

    @Override
    @Transactional
    public void addRows(Long runId, int count) {
        if (runId == null || count <= 0) {
            return;
        }
        ResultRun run = get(runId);
        run.setRowCount(run.getRowCount() + count);
        run.setLastActivityAt(Timestamp.from(Instant.now()));
        update(run);
    }

    @Override
    @Transactional
    public void rowLeftStaging(Long runId, String sysUserId) {
        if (runId == null) {
            return;
        }
        Optional<ResultRun> found = baseObjectDAO.get(runId);
        if (found.isEmpty()) {
            return;
        }
        ResultRun run = found.get();
        Timestamp now = Timestamp.from(Instant.now());
        run.setLastActivityAt(now);
        boolean completed = run.getSource() == ResultRunSource.ANALYZER && run.getState().isActive()
                && baseObjectDAO.countStagedRows(runId) == 0;
        if (completed) {
            run.setState(ResultRunState.COMPLETE);
            run.setCompletedAt(now);
        }
        update(run);
        if (completed) {
            runQcService.supersedeIfRetested(runId, sysUserId != null ? sysUserId : run.getSysUserId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResultRunListResponse listActiveRuns(RunViewer viewer, String testSectionId) {
        List<ResultRun> runs = baseObjectDAO.getActiveRuns();
        Map<Long, List<AnalyzerResults>> rowsByRun = stagedRowsByRun(runs);
        Map<String, List<String>> unitsByAnalyzer = new HashMap<>();
        Map<String, String> unitByTest = new HashMap<>();
        List<ResultRunSummary> summaries = new ArrayList<>();
        int open = 0;
        int held = 0;
        for (ResultRun run : runs) {
            if (run.getSource() == ResultRunSource.WORKPLAN) {
                if (!unitsVisible(workplanUnits(run), viewer)
                        || (StringUtils.isNotBlank(testSectionId) && !testSectionId.equals(run.getTestSectionId()))) {
                    continue;
                }
                if (run.getState() == ResultRunState.HELD) {
                    held++;
                } else {
                    open++;
                }
                summaries.add(new ResultRunSummary(run.getId(), run.getLabel(), run.getSource().name(),
                        run.getState().name(), analyzerName(run.getAnalyzerId()), iso(run.getOpenedAt()),
                        workplanDispositioned(run), run.getRowCount()));
                continue;
            }
            List<String> analyzerUnits = unitsByAnalyzer.computeIfAbsent(String.valueOf(run.getAnalyzerId()),
                    id -> analyzerUnits(run.getAnalyzerId()));
            List<AnalyzerResults> staged = rowsByRun.getOrDefault(run.getId(), List.of());
            List<AnalyzerResults> visible = staged.stream().filter(row -> isVisible(row, analyzerUnits, viewer))
                    .toList();
            if (visible.isEmpty()) {
                continue;
            }
            if (StringUtils.isNotBlank(testSectionId)
                    && visible.stream().noneMatch(row -> inLabUnit(row, analyzerUnits, testSectionId, unitByTest))) {
                continue;
            }
            if (run.getState() == ResultRunState.HELD) {
                held++;
            } else {
                open++;
            }
            summaries.add(new ResultRunSummary(run.getId(), run.getLabel(), run.getSource().name(),
                    run.getState().name(), analyzerName(run.getAnalyzerId()), iso(run.getOpenedAt()),
                    Math.max(0, run.getRowCount() - staged.size()), run.getRowCount()));
        }
        return new ResultRunListResponse(open, held, summaries);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRunHeader> getHeader(Long runId, RunViewer viewer) {
        return visibleRun(runId, viewer).map(seen -> {
            ResultRun run = seen.run();
            if (run.getSource() == ResultRunSource.WORKPLAN) {
                return workplanHeader(seen);
            }
            List<ClassifiedRow> classified = classifyVisible(seen, viewer);
            Optional<Analyzer> analyzer = run.getAnalyzerId() == null ? Optional.empty()
                    : analyzerService.getWithMapping(run.getAnalyzerId());
            String method = analyzer.map(Analyzer::getMapping).map(ResultRunServiceImpl::typeNameOf).orElse(null);
            List<String> unitIds = run.getTestSectionId() != null ? List.of(run.getTestSectionId())
                    : seen.analyzerUnits();
            List<String> labUnits = unitIds.stream().map(this::labUnitName).filter(Objects::nonNull).toList();
            return new ResultRunHeader(run.getId(), run.getLabel(), run.getSource().name(), run.getState().name(),
                    run.getAnalyzerId(), analyzer.map(Analyzer::getName).orElse(null), method, labUnits,
                    run.getOperatorName(), iso(run.getOpenedAt()), iso(run.getLastActivityAt()),
                    iso(run.getCompletedAt()), Math.max(0, run.getRowCount() - seen.staged().size()), run.getRowCount(),
                    run.getDeliveryReceiptId() != null, chipCounts(classified), runQcService.qcBlock(run.getId()),
                    runExceptionService.exceptionsOf(run.getId(), run.getAnalyzerId(), classified),
                    lotNumberOf(run.getReagentLotId()), run.getBatchWorkplanId());
        });
    }

    /**
     * FR-B2, FR-E3: a workplan run's header. Its rows are its batch's analyses
     * still waiting for a result; its lab unit, instrument, operator and reagent
     * lot are the batch's.
     */
    private ResultRunHeader workplanHeader(VisibleRun seen) {
        ResultRun run = seen.run();
        List<ResultRunRow> rows = workplanRows(run);
        int heldRows = (int) rows.stream().filter(row -> row.category() == ResultRunRowCategory.HELD).count();
        ResultRunChipCounts counts = new ResultRunChipCounts(rows.size(), 0, 0, 0, 0, heldRows, rows.size() - heldRows);
        List<String> labUnits = workplanUnits(run).stream().map(this::labUnitName).filter(Objects::nonNull).toList();
        return new ResultRunHeader(run.getId(), run.getLabel(), run.getSource().name(), run.getState().name(),
                run.getAnalyzerId(), analyzerName(run.getAnalyzerId()), null, labUnits, run.getOperatorName(),
                iso(run.getOpenedAt()), iso(run.getLastActivityAt()), iso(run.getCompletedAt()),
                workplanDispositioned(run), run.getRowCount(), false, counts, runQcService.qcBlock(run.getId()),
                List.of(), lotNumberOf(run.getReagentLotId()), run.getBatchWorkplanId());
    }

    /**
     * The batch's analyses that still wait for a result, as run rows: entered
     * through the Results Entry panel and recorded into the run (FR-D5), held when
     * the run holds their test. A closed run, or an earlier run of a batch released
     * again, has none: those analyses are no longer in it.
     */
    private List<ResultRunRow> workplanRows(ResultRun run) {
        List<String> ids = baseObjectDAO.getBatchAnalysisIdsAwaiting(run.getId(),
                statusService.getStatusID(AnalysisStatus.Canceled), null);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, RunHoldCause> holds = runQcService.holdCauses(run.getId());
        Set<String> noControl = runQcService.testVerdicts(run.getId()).entrySet().stream()
                .filter(entry -> entry.getValue() == RunTestVerdict.NO_CONTROL).map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Map<String, String> names = new HashMap<>();
        List<ResultRunRow> rows = new ArrayList<>();
        for (Analysis analysis : analysisService.getAnalysesByIdsWithDetails(ids)) {
            String testId = analysis.getTest() == null ? null : analysis.getTest().getId();
            RunHoldCause hold = testId == null ? null : holds.get(testId);
            String accession = analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null ? null
                    : analysis.getSampleItem().getSample().getAccessionNumber();
            String name = testId == null ? null : names.computeIfAbsent(testId, rowClassifier::catalogNameOf);
            rows.add(new ResultRunRow(WORKPLAN_ROW_PREFIX + analysis.getId(), accession, name, null, null, null, false,
                    false, null, null, null, null,
                    hold != null ? ResultRunRowCategory.HELD : ResultRunRowCategory.NORMAL, testId,
                    testId == null ? null : componentOf(testId, null), null, null, null, hold,
                    hold == null && noControl.contains(testId)));
        }
        return rows;
    }

    private int workplanDispositioned(ResultRun run) {
        return baseObjectDAO.countResultedAnalyses(run.getId());
    }

    private static List<String> workplanUnits(ResultRun run) {
        return run.getTestSectionId() == null ? List.of() : List.of(run.getTestSectionId());
    }

    private String lotNumberOf(Long reagentLotId) {
        if (reagentLotId == null) {
            return null;
        }
        return inventoryLotService.getMatch("id", reagentLotId).map(InventoryLot::getLotNumber).orElse(null);
    }

    @Override
    @Transactional
    public ResultRun openWorkplanRun(Long batchWorkplanId, String testSectionId, String instrumentId, Long reagentLotId,
            String operatorName, String sysUserId) {
        Timestamp now = Timestamp.from(Instant.now());
        ResultRun run = baseObjectDAO.findBatchRun(batchWorkplanId).orElse(null);
        if (run != null && (run.getState() == ResultRunState.SUPERSEDED || runQcService.hasDecisions(run.getId()))) {
            closeRun(run, now, sysUserId);
            run = null;
        }
        boolean created = run == null;
        if (created) {
            run = new ResultRun();
            run.setSource(ResultRunSource.WORKPLAN);
            run.setBatchWorkplanId(batchWorkplanId);
            run.setOpenedAt(now);
            run.setState(ResultRunState.OPEN);
        } else if (!run.getState().isActive()) {
            run.setState(ResultRunState.OPEN);
            run.setCompletedAt(null);
        }
        run.setTestSectionId(StringUtils.trimToNull(testSectionId));
        run.setAnalyzerId(StringUtils.trimToNull(instrumentId));
        run.setReagentLotId(reagentLotId);
        run.setOperatorName(StringUtils.trimToNull(operatorName));
        run.setLastActivityAt(now);
        run.setSysUserId(sysUserId);
        if (created) {
            run.setId(insert(run));
        }
        run.setRowCount(baseObjectDAO.countResultedAnalyses(run.getId()) + (int) baseObjectDAO
                .countBatchAnalysesAwaiting(run.getId(), statusService.getStatusID(AnalysisStatus.Canceled)));
        update(run);
        runQcService.refreshState(run.getId(), sysUserId);
        return run;
    }

    @Override
    @Transactional
    public void completeWorkplanRun(Long batchWorkplanId, String sysUserId) {
        baseObjectDAO.findBatchRun(batchWorkplanId)
                .ifPresent(run -> closeRun(run, Timestamp.from(Instant.now()), sysUserId));
    }

    @Override
    @Transactional
    public void closeDecidedWorkplanRun(Long batchWorkplanId, String sysUserId) {
        baseObjectDAO.findBatchRun(batchWorkplanId).filter(run -> runQcService.hasDecisions(run.getId()))
                .ifPresent(run -> closeRun(run, Timestamp.from(Instant.now()), sysUserId));
    }

    private void closeRun(ResultRun run, Timestamp now, String sysUserId) {
        if (!run.getState().isActive()) {
            return;
        }
        run.setState(ResultRunState.COMPLETE);
        run.setCompletedAt(now);
        run.setLastActivityAt(now);
        run.setSysUserId(sysUserId);
        update(run);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRun> batchRun(Long batchWorkplanId) {
        return baseObjectDAO.findBatchRun(batchWorkplanId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> activeBatchRunsOf(Collection<String> analysisIds) {
        Map<String, Long> runs = new HashMap<>();
        for (Object[] row : baseObjectDAO.getActiveBatchRunsOfAnalyses(analysisIds)) {
            runs.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return runs;
    }

    @Override
    @Transactional
    public void workplanResultSaved(Long runId, String sysUserId) {
        ResultRun run = runId == null ? null : baseObjectDAO.get(runId).orElse(null);
        if (run == null || run.getSource() != ResultRunSource.WORKPLAN || !run.getState().isActive()) {
            return;
        }
        Timestamp now = Timestamp.from(Instant.now());
        run.setLastActivityAt(now);
        run.setSysUserId(sysUserId);
        if (baseObjectDAO.countBatchAnalysesAwaiting(runId, statusService.getStatusID(AnalysisStatus.Canceled)) == 0) {
            run.setState(ResultRunState.COMPLETE);
            run.setCompletedAt(now);
        }
        update(run);
    }

    @Override
    public IResultUpdate recordIntoWorkplanRuns(Collection<Result> results, String sysUserId) {
        List<Result> unplaced = results == null ? List.of()
                : results.stream().filter(result -> result != null && result.getRunId() == null
                        && result.getAnalysis() != null && result.getAnalysis().getId() != null).toList();
        Map<String, Long> runs = unplaced.isEmpty() ? Map.of()
                : activeBatchRunsOf(unplaced.stream().map(result -> result.getAnalysis().getId()).distinct().toList());
        Set<Long> touched = new LinkedHashSet<>();
        for (Result result : unplaced) {
            Long runId = runs.get(result.getAnalysis().getId());
            if (runId != null) {
                result.setRunId(runId);
                touched.add(runId);
            }
        }
        return new IResultUpdate() {
            @Override
            public void transactionalUpdate(IResultSaveService saveService) {
                touched.forEach(runId -> workplanResultSaved(runId, sysUserId));
            }

            @Override
            public void postTransactionalCommitUpdate(IResultSaveService saveService) {
            }
        };
    }

    @Override
    @Transactional
    public void refreshQcState(Long runId, String sysUserId) {
        if (runId != null) {
            runQcService.refreshState(runId, sysUserId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canSee(Long runId, RunViewer viewer) {
        return visibleRun(runId, viewer).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<ClassifiedRow>> classifiedRows(Long runId, RunViewer viewer) {
        return visibleRun(runId, viewer).map(seen -> classifyVisible(seen, viewer));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRunRowsPage> getRows(Long runId, RunViewer viewer, ResultRunChip chip, int page,
            int pageSize) {
        return visibleRun(runId, viewer).map(seen -> {
            int size = PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
            if (seen.run().getSource() == ResultRunSource.WORKPLAN) {
                List<ResultRunRow> matching = workplanRows(seen.run()).stream()
                        .filter(row -> chip.matches(row.category())).toList();
                int pages = Math.max(1, (matching.size() + size - 1) / size);
                int current = Math.min(Math.max(page, 1), pages);
                return new ResultRunRowsPage(matching.stream().skip((long) (current - 1) * size).limit(size).toList(),
                        current, size, matching.size());
            }
            List<ClassifiedRow> matching = inReviewOrder(classifyVisible(seen, viewer)).stream()
                    .filter(row -> chip.matches(row.category())).toList();
            int pages = Math.max(1, (matching.size() + size - 1) / size);
            int current = Math.min(Math.max(page, 1), pages);
            Map<String, String> testNames = new HashMap<>();
            List<ResultRunRow> rows = matching.stream().skip((long) (current - 1) * size).limit(size)
                    .map(row -> toRow(row, testNames)).toList();
            return new ResultRunRowsPage(rows, current, size, matching.size());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRunSelectionSummary> summarizeSelection(Long runId, RunViewer viewer,
            ResultRunSelection selection) {
        return visibleRun(runId, viewer).map(seen -> {
            ResultRunChip chip = ResultRunChip.fromRequest(selection.chip());
            Set<String> ids = selection.ids() == null ? Set.of() : Set.copyOf(selection.ids());
            Set<String> excluded = selection.excluded() == null ? Set.of() : Set.copyOf(selection.excluded());
            List<ClassifiedRow> selected = inReviewOrder(classifyVisible(seen, viewer)).stream()
                    .filter(row -> ids.contains(row.row().getId())
                            || selection.all() && chip.matches(row.category()) && !excluded.contains(row.row().getId()))
                    .toList();
            Map<String, String> testNames = new HashMap<>();
            List<ResultRunSelectionSummary.SkippedRow> skipped = selected.stream()
                    .filter(row -> SKIPPED.contains(row.category()))
                    .map(row -> new ResultRunSelectionSummary.SkippedRow(row.row().getId(),
                            row.row().getAccessionNumber(), rowClassifier.testNameOf(row.row(), testNames),
                            row.category()))
                    .toList();
            return new ResultRunSelectionSummary(selected.size(), count(selected, ResultRunRowCategory.CRITICAL),
                    count(selected, ResultRunRowCategory.ABNORMAL), count(selected, ResultRunRowCategory.NORMAL),
                    skipped);
        });
    }

    private static final Set<ResultRunRowCategory> SKIPPED = Set.of(ResultRunRowCategory.EXCEPTION,
            ResultRunRowCategory.HELD, ResultRunRowCategory.CONTROL);

    private List<ClassifiedRow> classifyVisible(VisibleRun seen, RunViewer viewer) {
        List<AnalyzerResults> visible = seen.staged().stream()
                .filter(row -> isVisible(row, seen.analyzerUnits(), viewer)).toList();
        return rowClassifier.classify(seen.run(), visible);
    }

    private static List<ClassifiedRow> inReviewOrder(List<ClassifiedRow> rows) {
        return rows.stream().sorted(Comparator.comparing(ClassifiedRow::category)).toList();
    }

    private static int count(List<ClassifiedRow> rows, ResultRunRowCategory category) {
        return (int) rows.stream().filter(row -> row.category() == category).count();
    }

    private static ResultRunChipCounts chipCounts(List<ClassifiedRow> rows) {
        return new ResultRunChipCounts(rows.size(),
                (int) rows.stream().filter(row -> ResultRunChip.NEEDS_REVIEW.matches(row.category())).count(),
                count(rows, ResultRunRowCategory.CRITICAL), count(rows, ResultRunRowCategory.ABNORMAL),
                count(rows, ResultRunRowCategory.EXCEPTION), count(rows, ResultRunRowCategory.HELD),
                count(rows, ResultRunRowCategory.NORMAL));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getRawMessage(Long runId, RunViewer viewer) {
        return visibleRun(runId, viewer).map(VisibleRun::run).map(ResultRun::getDeliveryReceiptId)
                .flatMap(receiptDAO::get).map(AnalyzerDeliveryReceipt::getBundleJson);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, WaitingRun> findWaiting(Collection<String> accessionNumbers) {
        List<String> accessions = accessionNumbers.stream().filter(StringUtils::isNotBlank).distinct().toList();
        List<AnalyzerResults> waiting = baseObjectDAO.getWaitingRowsForAccessions(accessions);
        Map<Long, WaitingRun> runs = new HashMap<>();
        Map<String, WaitingRun> byKey = new LinkedHashMap<>();
        Map<Long, Map<String, RunHoldCause>> heldTests = new HashMap<>();
        for (AnalyzerResults row : waiting) {
            byKey.putIfAbsent(ResultRunService.waitingKey(row.getAccessionNumber(), row.getTestId()),
                    waitingRun(row, runs, heldTests));
        }
        return byKey;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WaitingRun> findWaiting(String accessionNumber, String testId) {
        if (StringUtils.isBlank(accessionNumber) || StringUtils.isBlank(testId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                findWaiting(List.of(accessionNumber)).get(ResultRunService.waitingKey(accessionNumber, testId)));
    }

    @Override
    @Transactional(readOnly = true)
    public void markWaiting(List<TestResultItem> items) {
        List<TestResultItem> pending = items.stream().filter(ResultRunServiceImpl::isPending).toList();
        if (pending.isEmpty()) {
            markHeldResults(items);
            return;
        }
        List<String> accessions = pending.stream().map(TestResultItem::getAccessionNumber)
                .filter(StringUtils::isNotBlank).distinct().toList();
        Map<Long, WaitingRun> runs = new HashMap<>();
        Map<String, WaitingRun> byPart = new HashMap<>();
        Map<Long, Map<String, RunHoldCause>> heldTests = new HashMap<>();
        for (AnalyzerResults row : baseObjectDAO.getWaitingRowsForAccessions(accessions)) {
            byPart.putIfAbsent(partKey(row.getAccessionNumber(), row.getTestId(), row.getComponentId()),
                    waitingRun(row, runs, heldTests));
        }
        for (TestResultItem item : pending) {
            item.setWaitingRun(
                    byPart.get(partKey(item.getAccessionNumber(), item.getTestId(), item.getTestResultComponentId())));
        }
        markBatched(pending, runs, heldTests);
        markHeldResults(items);
    }

    /**
     * FR-D5: a pending row whose analysis is batched in an open or held workplan
     * run carries that run, so it reads "Batched in RUN-x" and its result records
     * into the run; held when the run holds its test.
     */
    private void markBatched(List<TestResultItem> pending, Map<Long, WaitingRun> runs,
            Map<Long, Map<String, RunHoldCause>> heldTests) {
        List<TestResultItem> unmarked = pending.stream()
                .filter(item -> item.getWaitingRun() == null && StringUtils.isNotBlank(item.getAnalysisId())).toList();
        if (unmarked.isEmpty()) {
            return;
        }
        Map<String, Long> batched = activeBatchRunsOf(
                unmarked.stream().map(TestResultItem::getAnalysisId).distinct().toList());
        for (TestResultItem item : unmarked) {
            Long runId = batched.get(item.getAnalysisId());
            if (runId != null) {
                item.setWaitingRun(runs.computeIfAbsent(runId, this::waitingRun)
                        .held(heldTests.computeIfAbsent(runId, runQcService::holdCauses).get(item.getTestId())));
            }
        }
    }

    /**
     * A row whose stored result came from a run that holds its test undecided
     * carries that run, held, so it is shown held and offers no entry or edit
     * (FR-C4, FR-D4). Once the test is decided the row is open to a new value.
     */
    private void markHeldResults(List<TestResultItem> items) {
        Map<String, Long> holding = runQcService
                .entryHoldingRuns(items.stream().filter(item -> item.getWaitingRun() == null)
                        .map(TestResultItem::getAnalysisId).filter(StringUtils::isNotBlank).distinct().toList());
        if (holding.isEmpty()) {
            return;
        }
        Map<Long, WaitingRun> runs = new HashMap<>();
        Map<Long, Map<String, RunHoldCause>> causes = new HashMap<>();
        for (TestResultItem item : items) {
            Long runId = item.getWaitingRun() == null ? holding.get(item.getAnalysisId()) : null;
            if (runId != null) {
                item.setWaitingRun(runs.computeIfAbsent(runId, this::waitingRun)
                        .held(causes.computeIfAbsent(runId, runQcService::holdCauses).getOrDefault(item.getTestId(),
                                RunHoldCause.QC_FAILED)));
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WaitingRun heldRun(Long runId, String testId) {
        Map<String, RunHoldCause> causes = runQcService.holdCauses(runId);
        RunHoldCause cause = testId != null ? causes.get(testId)
                : causes.values().stream().distinct().count() == 1 ? causes.values().iterator().next() : null;
        return waitingRun(runId).held(cause == null ? RunHoldCause.QC_FAILED : cause);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WaitingRun> waitingFor(Analysis analysis, String componentId) {
        if (analysis == null || analysis.getTest() == null || analysis.getSampleItem() == null
                || analysis.getSampleItem().getSample() == null) {
            return Optional.empty();
        }
        String testId = analysis.getTest().getId();
        String component = componentOf(testId, componentId);
        boolean resulted = resultService.getResultsByAnalysis(analysis).stream()
                .filter(result -> StringUtils.isNotBlank(result.getValue()))
                .anyMatch(result -> Objects.equals(component, componentOf(testId,
                        result.getTestResult() == null ? null : result.getTestResult().getComponentId())));
        if (resulted) {
            return Optional.empty();
        }
        return baseObjectDAO
                .getWaitingRowsForAccessions(List.of(analysis.getSampleItem().getSample().getAccessionNumber()))
                .stream()
                .filter(row -> testId.equals(row.getTestId())
                        && Objects.equals(component, componentOf(testId, row.getComponentId())))
                .findFirst().map(row -> waitingRun(row, new HashMap<>(), new HashMap<>()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AnalyzerResults> stagedValueFor(String runRowId, Analysis analysis, String componentId) {
        if (StringUtils.isBlank(runRowId) || analysis == null || analysis.getTest() == null
                || analysis.getSampleItem() == null || analysis.getSampleItem().getSample() == null) {
            return Optional.empty();
        }
        String accessionNumber = analysis.getSampleItem().getSample().getAccessionNumber();
        return analyzerResultsDAO.get(runRowId).filter(row -> row.getRunId() != null && !row.getIsControl()
                && row.getDuplicateAnalyzerResultId() == null && StringUtils.isBlank(row.getImportIssueReason()))
                .filter(row -> Objects.equals(accessionNumber, row.getAccessionNumber())
                        && Objects.equals(analysis.getTest().getId(), row.getTestId())
                        && Objects.equals(componentOf(row.getTestId(), componentId),
                                componentOf(row.getTestId(), row.getComponentId())))
                .filter(row -> baseObjectDAO.get(row.getRunId())
                        .filter(run -> run.getState() != null && run.getState().isActive()).isPresent());
    }

    static boolean isPending(TestResultItem item) {
        String multi = item.getMultiSelectResultValues();
        return StringUtils.isBlank(item.getResultValue()) && (StringUtils.isBlank(multi) || "{}".equals(multi));
    }

    private record VisibleRun(ResultRun run, List<AnalyzerResults> staged, List<String> analyzerUnits) {
    }

    /**
     * A run is seen by whoever may see one of its values, or, once no value is
     * left, whoever works a lab unit of its analyzer.
     */
    private Optional<VisibleRun> visibleRun(Long runId, RunViewer viewer) {
        if (runId == null) {
            return Optional.empty();
        }
        return baseObjectDAO.get(runId).flatMap(run -> {
            if (run.getSource() == ResultRunSource.WORKPLAN) {
                List<String> units = workplanUnits(run);
                return unitsVisible(units, viewer) ? Optional.of(new VisibleRun(run, List.of(), units))
                        : Optional.empty();
            }
            List<String> units = analyzerUnits(run.getAnalyzerId());
            List<AnalyzerResults> staged = baseObjectDAO.getStagedRows(List.of(run.getId()));
            boolean seen = staged.isEmpty() ? unitsVisible(units, viewer)
                    : staged.stream().anyMatch(row -> isVisible(row, units, viewer));
            return seen ? Optional.of(new VisibleRun(run, staged, units)) : Optional.empty();
        });
    }

    private Map<Long, List<AnalyzerResults>> stagedRowsByRun(List<ResultRun> runs) {
        return baseObjectDAO.getStagedRows(runs.stream().map(ResultRun::getId).toList()).stream()
                .collect(Collectors.groupingBy(AnalyzerResults::getRunId));
    }

    /**
     * D-043 inside a run: a value of a test shows to those who result that test; a
     * value no test is matched to yet shows to those who work a lab unit of the
     * analyzer.
     */
    static boolean isVisible(AnalyzerResults row, List<String> analyzerUnits, RunViewer viewer) {
        if (row.getTestId() != null) {
            return viewer.testIds().contains(row.getTestId());
        }
        return unitsVisible(analyzerUnits, viewer);
    }

    static boolean unitsVisible(List<String> analyzerUnits, RunViewer viewer) {
        if (viewer.labUnitIds().isEmpty()) {
            return false;
        }
        return analyzerUnits.isEmpty() || analyzerUnits.stream().anyMatch(viewer.labUnitIds()::contains);
    }

    private boolean inLabUnit(AnalyzerResults row, List<String> analyzerUnits, String testSectionId,
            Map<String, String> unitByTest) {
        if (row.getTestId() == null) {
            return analyzerUnits.contains(testSectionId);
        }
        return testSectionId.equals(unitByTest.computeIfAbsent(row.getTestId(), this::labUnitOfTest));
    }

    private String labUnitOfTest(String testId) {
        Test test = testService.get(testId);
        return test == null || test.getTestSection() == null ? "" : test.getTestSection().getId();
    }

    private List<String> analyzerUnits(String analyzerId) {
        if (analyzerId == null) {
            return List.of();
        }
        return analyzerService.getWithMapping(analyzerId).map(Analyzer::getTestUnitIds).map(List::copyOf)
                .orElse(List.of());
    }

    private String analyzerName(String analyzerId) {
        if (analyzerId == null) {
            return null;
        }
        return analyzerService.getWithMapping(analyzerId).map(Analyzer::getName).orElse(null);
    }

    /**
     * Run settings' Method is the analyzer type (FR-E3), named as the type catalog
     * names it: the name the mapping kept from the type it was made from, or the
     * type's id for a mapping older than that.
     */
    static String typeNameOf(AnalyzerMapping mapping) {
        if (StringUtils.isNotBlank(mapping.getSourceDefinitionJson())) {
            String name = AnalyzerMappingDefinition.of(mapping).displayName();
            if (StringUtils.isNotBlank(name)) {
                return name;
            }
        }
        return mapping.getProfileId();
    }

    private String labUnitName(String testSectionId) {
        TestSection section = testSectionService.getTestSectionById(testSectionId);
        if (section == null) {
            return null;
        }
        String localized = testSectionService.getUserLocalizedTesSectionName(section);
        return StringUtils.isNotBlank(localized) ? localized : section.getTestSectionName();
    }

    private ResultRunRow toRow(ClassifiedRow classified, Map<String, String> testNames) {
        AnalyzerResults row = classified.row();
        String value = StringUtils.isNotBlank(row.getRawResultValue()) ? row.getRawResultValue() : row.getResult();
        String part = StringUtils.isBlank(row.getComponentId()) ? null
                : testResultComponentService.getMatch("id", row.getComponentId())
                        .map(TestResultComponent::getLocalizedLabel).orElse(null);
        return new ResultRunRow(row.getId(), row.getAccessionNumber(), rowClassifier.testNameOf(row, testNames), part,
                value, row.getUnits(), row.getIsControl(), row.getDuplicateAnalyzerResultId() != null,
                row.getImportIssueReason(), row.getInstrumentFlags(), iso(row.getCompleteDate()), classified.flag(),
                classified.category(), row.getTestId(),
                row.getTestId() == null ? StringUtils.trimToNull(row.getComponentId())
                        : componentOf(row.getTestId(), row.getComponentId()),
                row.getResult(), classified.exceptionKind(), codeOf(row), classified.holdCause(),
                classified.missingControl());
    }

    /** What the analyzer called the value's test. */
    static String codeOf(AnalyzerResults row) {
        return StringUtils.defaultIfBlank(row.getRawTestCode(), row.getTestName());
    }

    private String partKey(String accessionNumber, String testId, String componentId) {
        return ResultRunService.waitingKey(accessionNumber, testId) + "|"
                + StringUtils.defaultString(componentOf(testId, componentId));
    }

    private WaitingRun waitingRun(AnalyzerResults row, Map<Long, WaitingRun> runs,
            Map<Long, Map<String, RunHoldCause>> heldTests) {
        WaitingRun run = runs.computeIfAbsent(row.getRunId(), this::waitingRun);
        return run.held(heldTests.computeIfAbsent(row.getRunId(), runQcService::holdCauses).get(row.getTestId()));
    }

    private WaitingRun waitingRun(Long runId) {
        ResultRun run = get(runId);
        return new WaitingRun(run.getId(), run.getLabel(), run.getSource().name(), iso(run.getOpenedAt()), false, null);
    }

    /**
     * The worklist row a value belongs to, named the way the worklist names it: a
     * test with one part has a single row and no part, and on a test with several a
     * value names its own part, or the primary one when it names none, the way the
     * import treats a test's main value.
     */
    private String componentOf(String testId, String componentId) {
        return rowClassifier.partOf(testId, componentId);
    }

    private static String iso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
