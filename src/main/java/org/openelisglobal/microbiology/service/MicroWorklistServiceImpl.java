package org.openelisglobal.microbiology.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.openelisglobal.microbiology.dao.MicroAstPanelDAO;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.dao.MicroWorklistContextDAO;
import org.openelisglobal.microbiology.form.MicroWorklistActivityContext;
import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.openelisglobal.microbiology.form.MicroWorklistRecentActivityContext;
import org.openelisglobal.microbiology.form.MicroWorklistRecentActivityForm;
import org.openelisglobal.microbiology.form.MicroWorklistRowForm;
import org.openelisglobal.microbiology.form.MicroWorklistSpecimenContext;
import org.openelisglobal.microbiology.form.MicroWorklistSummaryForm;
import org.openelisglobal.microbiology.valueholder.MicroAstPanel;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.openelisglobal.microbiology.valueholder.MicroWorkflowType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroWorklistServiceImpl implements MicroWorklistService {

    private static final String CULTURES_GRAIN = "cultures";
    private static final String AST_GRAIN = "ast";
    private static final int RECENT_ACTIVITY_LIMIT = 25;
    private static final List<String> RESISTANCE_FLAGS = List.of("ESBL", "MRSA", "CRE", "VRE", "MDR");

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroCriticalCommunicationDAO communicationDAO;
    private final MicroWorklistContextDAO contextDAO;
    private final MicroAstPanelDAO panelDAO;

    public MicroWorklistServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO,
            MicroCriticalCommunicationDAO communicationDAO, MicroWorklistContextDAO contextDAO,
            MicroAstPanelDAO panelDAO) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.communicationDAO = communicationDAO;
        this.contextDAO = contextDAO;
        this.panelDAO = panelDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroWorklistPageForm getWorklistPage(MicroWorklistQueryForm query) {
        MicroWorklistQueryForm normalized = normalize(query);
        List<MicroCase> openCases = caseDAO.getOpenCases();
        List<String> caseIds = openCases.stream().map(MicroCase::getId).toList();
        List<String> sampleItemIds = openCases.stream().map(MicroCase::getSampleItemId).distinct().toList();
        Map<String, List<MicroCase>> casesBySampleItem = groupBy(caseDAO.getBySampleItemIds(sampleItemIds),
                MicroCase::getSampleItemId);
        Map<String, List<MicroIsolate>> isolatesByCase = groupBy(isolateDAO.getByCaseIds(caseIds),
                MicroIsolate::getCaseId);
        List<String> isolateIds = isolatesByCase.values().stream().flatMap(List::stream).map(MicroIsolate::getId)
                .toList();
        Map<String, List<MicroAstRun>> runsByIsolate = groupBy(
                isolateIds.isEmpty() ? List.of() : astRunDAO.getByIsolateIds(isolateIds), MicroAstRun::getIsolateId);
        Map<String, List<MicroCriticalCommunication>> communicationsByCase = groupBy(
                communicationDAO.getByCaseIds(caseIds), MicroCriticalCommunication::getCaseId);
        Map<String, MicroWorklistSpecimenContext> specimenContextBySampleItem = indexBy(
                contextDAO.getSpecimenContexts(sampleItemIds), MicroWorklistSpecimenContext::sampleItemId);
        Map<String, MicroWorklistActivityContext> activityContextByCase = indexBy(
                contextDAO.getLatestActivityContexts(caseIds), MicroWorklistActivityContext::caseId);
        List<MicroWorklistRecentActivityContext> recentActivityContexts = contextDAO.getRecentActivityContexts(caseIds,
                RECENT_ACTIVITY_LIMIT);
        List<String> panelIds = runsByIsolate.values().stream().flatMap(List::stream).map(MicroAstRun::getPanelId)
                .filter(panelId -> panelId != null && !panelId.isBlank()).distinct().toList();
        Map<String, MicroAstPanel> panelsById = indexBy(panelDAO.getByIds(panelIds), MicroAstPanel::getId);
        List<MicroWorklistRowForm> rows = AST_GRAIN.equals(normalized.grain)
                ? toAstRows(openCases, isolatesByCase, runsByIsolate)
                : toCultureRows(openCases, isolatesByCase, runsByIsolate, communicationsByCase, casesBySampleItem);
        enrichRows(rows, specimenContextBySampleItem, activityContextByCase, panelsById);
        List<MicroWorklistRowForm> summaryRows = new ArrayList<>(rows);
        summaryRows.removeIf(row -> !matches(row, queryWithoutActionFilters(normalized)));
        rows.removeIf(row -> !matches(row, normalized));
        rows.sort(comparatorFor(normalized.sort));

        MicroWorklistPageForm page = new MicroWorklistPageForm();
        page.summary = summarize(summaryRows);
        addResistanceHits(page.summary, runsByIsolate);
        page.recentActivity
                .addAll(toRecentActivityForms(recentActivityContexts, openCases, specimenContextBySampleItem));
        page.total = rows.size();
        page.page = normalized.page;
        page.pageSize = normalized.pageSize;
        int firstRow = Math.min((page.page - 1) * page.pageSize, rows.size());
        int lastRow = Math.min(firstRow + page.pageSize, rows.size());
        page.rows.addAll(rows.subList(firstRow, lastRow));
        return page;
    }

    private MicroWorklistQueryForm queryWithoutActionFilters(MicroWorklistQueryForm query) {
        MicroWorklistQueryForm summaryQuery = new MicroWorklistQueryForm();
        summaryQuery.grain = query.grain;
        summaryQuery.workflow = query.workflow;
        summaryQuery.urgency = query.urgency;
        summaryQuery.q = query.q;
        summaryQuery.sort = query.sort;
        summaryQuery.page = query.page;
        summaryQuery.pageSize = query.pageSize;
        return summaryQuery;
    }

    private MicroWorklistSummaryForm summarize(List<MicroWorklistRowForm> rows) {
        MicroWorklistSummaryForm summary = new MicroWorklistSummaryForm();
        summary.totalPending = rows.size();
        for (MicroWorklistRowForm row : rows) {
            if (AST_GRAIN.equals(row.grain)) {
                summarizeAstRow(summary, row);
                continue;
            }
            if (MicroCaseStage.INCUBATING.name().equals(row.stage)) {
                summary.incubating++;
            }
            if (MicroCaseStage.POSITIVE_SIGNAL.name().equals(row.stage)) {
                summary.positiveSignals++;
            }
            if (MicroCaseStage.GROWTH_DETECTED.name().equals(row.stage)) {
                summary.growthDetected++;
            }
            if (MicroCaseStage.IDENTIFICATION.name().equals(row.stage)) {
                summary.identification++;
            }
            if (row.needsAstReview) {
                summary.needsAstReview++;
            }
            if ("CASE_REVIEW".equals(row.dueAction)) {
                summary.readyForCaseReview++;
            }
            if (row.hasOpenCriticalCommunication) {
                summary.openCriticalFollowUps++;
            }
        }
        return summary;
    }

    private void summarizeAstRow(MicroWorklistSummaryForm summary, MicroWorklistRowForm row) {
        summary.astInQueue++;
        if ("PENDING_SETUP".equals(row.astStatus)) {
            summary.astPendingSetup++;
        } else if (MicroAstRunStatus.RESULTS_IN.name().equals(row.astStatus)
                || MicroAstRunStatus.QC_FAILED.name().equals(row.astStatus)) {
            summary.astResultsIn++;
        } else {
            summary.astInProgress++;
        }
        if (MicroAstRunStatus.AWAITING_RESULTS.name().equals(row.astStatus)) {
            summary.astAwaitingResults++;
        }
    }

    private void addResistanceHits(MicroWorklistSummaryForm summary, Map<String, List<MicroAstRun>> runsByIsolate) {
        RESISTANCE_FLAGS.forEach(flag -> summary.resistanceHits.put(flag, 0));
        LocalDate today = LocalDate.now();
        for (MicroAstRun run : runsByIsolate.values().stream().flatMap(List::stream).toList()) {
            java.sql.Timestamp flagTime = run.getAnalyzerCompletedAt() == null ? run.getAnalyzerLoadedAt()
                    : run.getAnalyzerCompletedAt();
            if (!isActiveWorklistRun(run) || flagTime == null
                    || !flagTime.toLocalDateTime().toLocalDate().equals(today)) {
                continue;
            }
            List<String> flags = List.of(safe(run.getAnalyzerExpertFlags()).toUpperCase(Locale.ROOT).split("\\|"));
            for (String flag : RESISTANCE_FLAGS) {
                if (flags.stream().map(String::trim).anyMatch(flag::equals)) {
                    summary.resistanceHits.compute(flag, (key, count) -> count + 1);
                }
            }
        }
    }

    private List<MicroWorklistRecentActivityForm> toRecentActivityForms(
            List<MicroWorklistRecentActivityContext> activityContexts, List<MicroCase> cases,
            Map<String, MicroWorklistSpecimenContext> specimenContextBySampleItem) {
        Map<String, MicroCase> casesById = indexBy(cases, MicroCase::getId);
        List<MicroWorklistRecentActivityForm> forms = new ArrayList<>();
        for (MicroWorklistRecentActivityContext activity : activityContexts) {
            MicroCase microCase = casesById.get(activity.caseId());
            if (microCase == null) {
                continue;
            }
            MicroWorklistSpecimenContext specimen = specimenContextBySampleItem.get(microCase.getSampleItemId());
            MicroWorklistRecentActivityForm form = new MicroWorklistRecentActivityForm();
            form.caseId = activity.caseId();
            form.accessionNumber = specimen == null ? microCase.getSampleItemId() : specimen.accessionNumber();
            form.activityType = activity.activityType();
            form.occurredAt = activity.occurredAt();
            form.performedByDisplay = activity.performedByDisplay();
            form.note = activity.note();
            forms.add(form);
        }
        return forms;
    }

    private MicroWorklistQueryForm normalize(MicroWorklistQueryForm query) {
        MicroWorklistQueryForm normalized = new MicroWorklistQueryForm();
        if (query == null) {
            return normalized;
        }
        normalized.grain = AST_GRAIN.equals(query.grain) ? AST_GRAIN : CULTURES_GRAIN;
        normalized.status = statusForGrain(normalized.grain, query.status);
        normalized.workflow = filterText(query.workflow);
        normalized.stage = filterText(query.stage);
        normalized.urgency = filterText(query.urgency);
        normalized.due = filterText(query.due);
        normalized.q = text(query.q);
        normalized.sort = query.sort != null && List.of("priority", "newest", "workflow").contains(query.sort)
                ? query.sort
                : "priority";
        normalized.page = Math.max(1, query.page);
        normalized.pageSize = Math.max(1, Math.min(100, query.pageSize));
        return normalized;
    }

    private boolean matches(MicroWorklistRowForm row, MicroWorklistQueryForm query) {
        if (!query.status.isEmpty() && !matchesStatus(row, query.status)) {
            return false;
        }
        if (!query.workflow.isEmpty() && !query.workflow.equals(row.workflowType)) {
            return false;
        }
        if (!query.stage.isEmpty() && !query.stage.equals(row.stage)) {
            return false;
        }
        if (!query.urgency.isEmpty() && !query.urgency.equals(row.urgency)) {
            return false;
        }
        if (!query.due.isEmpty() && !query.due.equals(row.dueAction)) {
            return false;
        }
        if (query.q.isEmpty()) {
            return true;
        }
        String searchable = String
                .join(" ", safe(row.caseId), safe(row.sampleItemId), safe(row.workflowType), safe(row.stage),
                        safe(row.dueAction), safe(row.urgency), safe(row.isolateLabel), safe(row.organismDisplay),
                        safe(row.panelId), safe(row.astStatus), safe(row.accessionNumber), safe(row.patientDisplay),
                        safe(row.specimenDisplay), safe(row.panelName), safe(row.lastActivityBy))
                .toLowerCase(Locale.ROOT);
        return searchable.contains(query.q.toLowerCase(Locale.ROOT));
    }

    private String statusForGrain(String grain, String value) {
        String status = text(value).toLowerCase(Locale.ROOT);
        List<String> allowed = AST_GRAIN.equals(grain) ? List.of("pending-setup", "in-progress", "results-in")
                : List.of("incubating", "positive", "growth", "ready");
        return allowed.contains(status) ? status : "";
    }

    private boolean matchesStatus(MicroWorklistRowForm row, String status) {
        if (AST_GRAIN.equals(row.grain)) {
            if ("pending-setup".equals(status)) {
                return "PENDING_SETUP".equals(row.astStatus);
            }
            if ("results-in".equals(status)) {
                return MicroAstRunStatus.RESULTS_IN.name().equals(row.astStatus)
                        || MicroAstRunStatus.QC_FAILED.name().equals(row.astStatus);
            }
            return !"PENDING_SETUP".equals(row.astStatus) && !MicroAstRunStatus.RESULTS_IN.name().equals(row.astStatus)
                    && !MicroAstRunStatus.QC_FAILED.name().equals(row.astStatus);
        }
        if ("incubating".equals(status)) {
            return MicroCaseStage.INCUBATING.name().equals(row.stage);
        }
        if ("positive".equals(status)) {
            return MicroCaseStage.POSITIVE_SIGNAL.name().equals(row.stage);
        }
        if ("growth".equals(status)) {
            return MicroCaseStage.GROWTH_DETECTED.name().equals(row.stage);
        }
        return MicroCaseStage.REVIEW_READY.name().equals(row.stage)
                || MicroCaseStage.NO_GROWTH_READY.name().equals(row.stage);
    }

    private Comparator<MicroWorklistRowForm> comparatorFor(String sort) {
        Comparator<MicroWorklistRowForm> priority = Comparator.comparingInt(this::urgencyRank)
                .thenComparingInt(this::actionRank)
                .thenComparing(row -> row.createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
        if ("newest".equals(sort)) {
            return Comparator.comparing((MicroWorklistRowForm row) -> row.createdAt,
                    Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(priority);
        }
        if ("workflow".equals(sort)) {
            return Comparator.comparing((MicroWorklistRowForm row) -> safe(row.workflowType)).thenComparing(priority);
        }
        return priority;
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private String filterText(String value) {
        String normalized = text(value);
        return "ALL".equalsIgnoreCase(normalized) ? "" : normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private MicroWorklistRowForm toRow(MicroCase microCase, List<MicroIsolate> isolates, List<MicroAstRun> runs,
            List<MicroCriticalCommunication> communications, List<MicroCase> siblingCases) {
        MicroWorklistRowForm row = new MicroWorklistRowForm();
        row.rowId = microCase.getId();
        row.grain = CULTURES_GRAIN;
        row.caseId = microCase.getId();
        row.sampleItemId = microCase.getSampleItemId();
        row.workflowType = microCase.getWorkflowType();
        row.stage = microCase.getStage();
        row.priority = microCase.getPriority();
        row.createdAt = microCase.getCreatedAt();
        row.needsAstReview = needsAstReview(runs);
        row.hasOpenCriticalCommunication = hasOpenCriticalCommunication(communications);
        row.dueAction = dueAction(microCase, isolates, row.needsAstReview);
        row.urgency = urgency(microCase, row.needsAstReview, row.hasOpenCriticalCommunication);
        for (MicroCase sibling : siblingCases) {
            if (!sibling.getId().equals(microCase.getId())) {
                row.siblingWorkflows.add(sibling.getWorkflowType());
            }
        }
        return row;
    }

    private List<MicroWorklistRowForm> toCultureRows(List<MicroCase> openCases,
            Map<String, List<MicroIsolate>> isolatesByCase, Map<String, List<MicroAstRun>> runsByIsolate,
            Map<String, List<MicroCriticalCommunication>> communicationsByCase,
            Map<String, List<MicroCase>> casesBySampleItem) {
        List<MicroWorklistRowForm> rows = new ArrayList<>();
        for (MicroCase microCase : openCases) {
            List<MicroIsolate> isolates = valuesFor(isolatesByCase, microCase.getId());
            rows.add(toRow(microCase, isolates, valuesFor(runsByIsolate, isolates),
                    valuesFor(communicationsByCase, microCase.getId()),
                    valuesFor(casesBySampleItem, microCase.getSampleItemId())));
        }
        return rows;
    }

    private List<MicroWorklistRowForm> toAstRows(List<MicroCase> openCases,
            Map<String, List<MicroIsolate>> isolatesByCase, Map<String, List<MicroAstRun>> runsByIsolate) {
        List<MicroWorklistRowForm> rows = new ArrayList<>();
        for (MicroCase microCase : openCases) {
            for (MicroIsolate isolate : valuesFor(isolatesByCase, microCase.getId())) {
                List<MicroAstRun> activeRuns = valuesFor(runsByIsolate, isolate.getId()).stream()
                        .filter(this::isActiveWorklistRun).toList();
                if (activeRuns.isEmpty()) {
                    if (MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name().equals(isolate.getSignificance())) {
                        rows.add(toAstRow(microCase, isolate, null));
                    }
                    continue;
                }
                for (MicroAstRun run : activeRuns) {
                    rows.add(toAstRow(microCase, isolate, run));
                }
            }
        }
        return rows;
    }

    private boolean isActiveWorklistRun(MicroAstRun run) {
        return !MicroAstRunStatus.INVALIDATED.name().equals(run.getStatus())
                && !MicroAstRunStatus.RERUN_REQUIRED.name().equals(run.getStatus())
                && !MicroAstRunStatus.CANCELLED.name().equals(run.getStatus());
    }

    private MicroWorklistRowForm toAstRow(MicroCase microCase, MicroIsolate isolate, MicroAstRun run) {
        MicroWorklistRowForm row = new MicroWorklistRowForm();
        row.grain = AST_GRAIN;
        row.caseId = microCase.getId();
        row.sampleItemId = microCase.getSampleItemId();
        row.workflowType = microCase.getWorkflowType();
        row.stage = microCase.getStage();
        row.priority = microCase.getPriority();
        row.isolateId = isolate.getId();
        row.isolateLabel = isolate.getIsolateLabel();
        row.organismDisplay = text(isolate.getPreliminaryOrganismText()).isEmpty() ? isolate.getOrganismId()
                : isolate.getPreliminaryOrganismText();
        if (run == null) {
            row.rowId = "setup:" + isolate.getId();
            row.astStatus = "PENDING_SETUP";
            row.dueAction = "AST_ENTRY";
            row.createdAt = isolate.getCreatedAt();
        } else {
            row.rowId = run.getId();
            row.astRunId = run.getId();
            row.panelId = run.getPanelId();
            row.astStatus = run.getStatus();
            row.astStartedAt = run.getStartedAt();
            row.createdAt = run.getStartedAt();
            row.analyzerResultsAvailable = MicroAstRunStatus.RESULTS_IN.name().equals(run.getStatus())
                    || MicroAstRunStatus.QC_FAILED.name().equals(run.getStatus());
            row.analyzerExpertFlags = run.getAnalyzerExpertFlags();
            row.dueAction = row.analyzerResultsAvailable ? "AST_REVIEW" : "AST_IN_PROGRESS";
        }
        row.needsAstReview = row.analyzerResultsAvailable;
        row.urgency = urgency(microCase, row.needsAstReview, false);
        return row;
    }

    private boolean needsAstReview(List<MicroAstRun> runs) {
        for (MicroAstRun run : runs) {
            if (!MicroAstRunStatus.REVIEWED.name().equals(run.getStatus())) {
                return true;
            }
        }
        return false;
    }

    private List<MicroAstRun> valuesFor(Map<String, List<MicroAstRun>> runsByIsolate, List<MicroIsolate> isolates) {
        return isolates.stream().flatMap(isolate -> valuesFor(runsByIsolate, isolate.getId()).stream()).toList();
    }

    private <T> List<T> valuesFor(Map<String, List<T>> valuesByKey, String key) {
        return valuesByKey.getOrDefault(key, List.of());
    }

    private <T> Map<String, List<T>> groupBy(List<T> values, Function<T, String> keyFunction) {
        return values.stream().collect(Collectors.groupingBy(keyFunction, LinkedHashMap::new, Collectors.toList()));
    }

    private <T> Map<String, T> indexBy(List<T> values, Function<T, String> keyFunction) {
        return values.stream().collect(
                Collectors.toMap(keyFunction, Function.identity(), (first, ignored) -> first, LinkedHashMap::new));
    }

    private void enrichRows(List<MicroWorklistRowForm> rows,
            Map<String, MicroWorklistSpecimenContext> specimenContextBySampleItem,
            Map<String, MicroWorklistActivityContext> activityContextByCase, Map<String, MicroAstPanel> panelsById) {
        for (MicroWorklistRowForm row : rows) {
            MicroWorklistSpecimenContext specimen = specimenContextBySampleItem.get(row.sampleItemId);
            if (specimen != null) {
                row.accessionNumber = specimen.accessionNumber();
                row.patientDisplay = specimen.patientDisplay();
                row.specimenDisplay = specimen.specimenDisplay();
            }
            MicroWorklistActivityContext activity = activityContextByCase.get(row.caseId);
            if (activity != null) {
                row.lastActivityAt = activity.occurredAt();
                row.lastActivityBy = activity.performedByDisplay();
            }
            MicroAstPanel panel = panelsById.get(row.panelId);
            if (panel != null) {
                row.panelName = panel.getName();
            }
        }
    }

    private boolean hasOpenCriticalCommunication(List<MicroCriticalCommunication> communications) {
        for (MicroCriticalCommunication communication : communications) {
            if (!MicroCriticalCommunicationStatus.CLOSED.name().equals(communication.getAcknowledgementStatus())
                    && Boolean.TRUE.equals(communication.getFollowUpNeeded())) {
                return true;
            }
        }
        return false;
    }

    private String dueAction(MicroCase microCase, List<MicroIsolate> isolates, boolean needsAstReview) {
        if (MicroWorkflowType.UNASSIGNED.name().equals(microCase.getWorkflowType())) {
            return "NEEDS_WORKFLOW";
        }
        if (needsAstReview) {
            return "AST_REVIEW";
        }
        if (MicroCaseStage.RECEIVED.name().equals(microCase.getStage())) {
            return "SETUP";
        }
        if (MicroCaseStage.POSITIVE_SIGNAL.name().equals(microCase.getStage())) {
            return "SUBCULTURE_GRAM_STAIN";
        }
        if (isolates.isEmpty()) {
            return "ISOLATE_ID";
        }
        for (MicroIsolate isolate : isolates) {
            if (MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name().equals(isolate.getSignificance())) {
                return "AST_ENTRY";
            }
        }
        return "CASE_REVIEW";
    }

    private String urgency(MicroCase microCase, boolean needsAstReview, boolean hasOpenCriticalCommunication) {
        if (needsAstReview || hasOpenCriticalCommunication || "STAT".equals(microCase.getPriority())
                || "URGENT".equals(microCase.getPriority())) {
            return "HIGH";
        }
        return "ROUTINE";
    }

    private int urgencyRank(MicroWorklistRowForm row) {
        return "HIGH".equals(row.urgency) ? 0 : 1;
    }

    private int actionRank(MicroWorklistRowForm row) {
        if ("NEEDS_WORKFLOW".equals(row.dueAction)) {
            return 0;
        }
        if ("AST_REVIEW".equals(row.dueAction)) {
            return 1;
        }
        if ("SETUP".equals(row.dueAction)) {
            return 2;
        }
        if ("ISOLATE_ID".equals(row.dueAction)) {
            return 3;
        }
        if ("AST_ENTRY".equals(row.dueAction)) {
            return 4;
        }
        return 5;
    }
}
