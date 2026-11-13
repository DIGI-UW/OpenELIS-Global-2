package org.openelisglobal.resultrun.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.dao.AnalyzerDeliveryReceiptDAO;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerDeliveryReceipt;
import org.openelisglobal.analyzerresults.dao.AnalyzerResultsDAO;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.openelisglobal.result.service.ResultService;
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
    @Autowired
    private ResultRunRowClassifier rowClassifier;
    @Lazy
    @Autowired
    private AnalyzerResultsDAO analyzerResultsDAO;

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
            if (!run.getState().isActive()) {
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
    public void rowLeftStaging(Long runId) {
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
        if (run.getSource() == ResultRunSource.ANALYZER && run.getState().isActive()
                && baseObjectDAO.countStagedRows(runId) == 0) {
            run.setState(ResultRunState.COMPLETE);
            run.setCompletedAt(now);
        }
        update(run);
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
            Optional<Analyzer> analyzer = run.getAnalyzerId() == null ? Optional.empty()
                    : analyzerService.getWithMapping(run.getAnalyzerId());
            String method = analyzer.map(Analyzer::getMapping).map(mapping -> mapping.getProfileId()).orElse(null);
            List<String> unitIds = run.getTestSectionId() != null ? List.of(run.getTestSectionId())
                    : seen.analyzerUnits();
            List<String> labUnits = unitIds.stream().map(this::labUnitName).filter(Objects::nonNull).toList();
            return new ResultRunHeader(run.getId(), run.getLabel(), run.getSource().name(), run.getState().name(),
                    run.getAnalyzerId(), analyzer.map(Analyzer::getName).orElse(null), method, labUnits,
                    run.getOperatorName(), iso(run.getOpenedAt()), iso(run.getLastActivityAt()),
                    iso(run.getCompletedAt()), Math.max(0, run.getRowCount() - seen.staged().size()), run.getRowCount(),
                    run.getDeliveryReceiptId() != null, chipCounts(classifyVisible(seen, viewer)));
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResultRunRowsPage> getRows(Long runId, RunViewer viewer, ResultRunChip chip, int page,
            int pageSize) {
        return visibleRun(runId, viewer).map(seen -> {
            int size = PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
            List<ClassifiedRow> matching = inReviewOrder(classifyVisible(seen, viewer)).stream()
                    .filter(row -> chip.matches(row.category())).toList();
            int pages = Math.max(1, (matching.size() + size - 1) / size);
            int current = Math.min(Math.max(page, 1), pages);
            List<ResultRunRow> rows = matching.stream().skip((long) (current - 1) * size).limit(size).map(this::toRow)
                    .toList();
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
            List<ResultRunSelectionSummary.SkippedRow> skipped = selected.stream()
                    .filter(row -> SKIPPED.contains(row.category()))
                    .map(row -> new ResultRunSelectionSummary.SkippedRow(row.row().getId(),
                            row.row().getAccessionNumber(), row.row().getTestName(), row.category()))
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
        for (AnalyzerResults row : waiting) {
            byKey.putIfAbsent(ResultRunService.waitingKey(row.getAccessionNumber(), row.getTestId()),
                    runs.computeIfAbsent(row.getRunId(), this::waitingRun));
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
            return;
        }
        List<String> accessions = pending.stream().map(TestResultItem::getAccessionNumber)
                .filter(StringUtils::isNotBlank).distinct().toList();
        Map<Long, WaitingRun> runs = new HashMap<>();
        Map<String, WaitingRun> byPart = new HashMap<>();
        for (AnalyzerResults row : baseObjectDAO.getWaitingRowsForAccessions(accessions)) {
            byPart.putIfAbsent(partKey(row.getAccessionNumber(), row.getTestId(), row.getComponentId()),
                    runs.computeIfAbsent(row.getRunId(), this::waitingRun));
        }
        for (TestResultItem item : pending) {
            item.setWaitingRun(
                    byPart.get(partKey(item.getAccessionNumber(), item.getTestId(), item.getTestResultComponentId())));
        }
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
                .findFirst().map(row -> waitingRun(row.getRunId()));
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
                .filter(row -> baseObjectDAO.get(row.getRunId()).filter(run -> run.getState() == ResultRunState.OPEN)
                        .isPresent());
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

    private String labUnitName(String testSectionId) {
        TestSection section = testSectionService.getTestSectionById(testSectionId);
        if (section == null) {
            return null;
        }
        String localized = testSectionService.getUserLocalizedTesSectionName(section);
        return StringUtils.isNotBlank(localized) ? localized : section.getTestSectionName();
    }

    private ResultRunRow toRow(ClassifiedRow classified) {
        AnalyzerResults row = classified.row();
        String value = StringUtils.isNotBlank(row.getRawResultValue()) ? row.getRawResultValue() : row.getResult();
        String part = StringUtils.isBlank(row.getComponentId()) ? null
                : testResultComponentService.getMatch("id", row.getComponentId()).map(TestResultComponent::getLabel)
                        .orElse(null);
        return new ResultRunRow(row.getId(), row.getAccessionNumber(), row.getTestName(), part, value, row.getUnits(),
                row.getIsControl(), row.getDuplicateAnalyzerResultId() != null, row.getImportIssueReason(),
                row.getInstrumentFlags(), iso(row.getCompleteDate()), classified.flag(), classified.category(),
                row.getTestId(), row.getTestId() == null ? StringUtils.trimToNull(row.getComponentId())
                        : componentOf(row.getTestId(), row.getComponentId()),
                row.getResult());
    }

    private String partKey(String accessionNumber, String testId, String componentId) {
        return ResultRunService.waitingKey(accessionNumber, testId) + "|"
                + StringUtils.defaultString(componentOf(testId, componentId));
    }

    private WaitingRun waitingRun(Long runId) {
        ResultRun run = get(runId);
        return new WaitingRun(run.getId(), run.getLabel(), run.getSource().name(), iso(run.getOpenedAt()));
    }

    /**
     * The worklist row a value belongs to, named the way the worklist names it: a
     * test with one part has a single row and no part, and on a test with several a
     * value names its own part, or the primary one when it names none, the way the
     * import treats a test's main value.
     */
    private String componentOf(String testId, String componentId) {
        if (testId == null) {
            return StringUtils.trimToNull(componentId);
        }
        List<TestResultComponent> components = testResultComponentService.getActiveComponentsByTestId(testId);
        if (components == null || components.size() <= 1) {
            return null;
        }
        String own = StringUtils.trimToNull(componentId);
        return own != null ? own
                : components.stream().filter(component -> Boolean.TRUE.equals(component.getIsPrimary()))
                        .map(TestResultComponent::getId).findFirst().orElse(null);
    }

    private static String iso(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant().toString();
    }
}
