package org.openelisglobal.microbiology.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.openelisglobal.microbiology.dao.MicroAstRunDAO;
import org.openelisglobal.microbiology.dao.MicroCaseDAO;
import org.openelisglobal.microbiology.dao.MicroCriticalCommunicationDAO;
import org.openelisglobal.microbiology.dao.MicroIsolateDAO;
import org.openelisglobal.microbiology.form.MicroWorklistPageForm;
import org.openelisglobal.microbiology.form.MicroWorklistQueryForm;
import org.openelisglobal.microbiology.form.MicroWorklistRowForm;
import org.openelisglobal.microbiology.form.MicroWorklistSummaryForm;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunStatus;
import org.openelisglobal.microbiology.valueholder.MicroCase;
import org.openelisglobal.microbiology.valueholder.MicroCaseStage;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunication;
import org.openelisglobal.microbiology.valueholder.MicroCriticalCommunicationStatus;
import org.openelisglobal.microbiology.valueholder.MicroIsolate;
import org.openelisglobal.microbiology.valueholder.MicroIsolateSignificance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroWorklistServiceImpl implements MicroWorklistService {

    private final MicroCaseDAO caseDAO;
    private final MicroIsolateDAO isolateDAO;
    private final MicroAstRunDAO astRunDAO;
    private final MicroCriticalCommunicationDAO communicationDAO;

    public MicroWorklistServiceImpl(MicroCaseDAO caseDAO, MicroIsolateDAO isolateDAO, MicroAstRunDAO astRunDAO,
            MicroCriticalCommunicationDAO communicationDAO) {
        this.caseDAO = caseDAO;
        this.isolateDAO = isolateDAO;
        this.astRunDAO = astRunDAO;
        this.communicationDAO = communicationDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public MicroWorklistPageForm getWorklistPage(MicroWorklistQueryForm query) {
        MicroWorklistQueryForm normalized = normalize(query);
        List<MicroWorklistRowForm> rows = new ArrayList<>();
        for (MicroCase microCase : caseDAO.getOpenCases()) {
            rows.add(toRow(microCase));
        }
        List<MicroWorklistRowForm> summaryRows = new ArrayList<>(rows);
        summaryRows.removeIf(row -> !matches(row, queryWithoutActionFilters(normalized)));
        rows.removeIf(row -> !matches(row, normalized));
        rows.sort(comparatorFor(normalized.sort));

        MicroWorklistPageForm page = new MicroWorklistPageForm();
        page.summary = summarize(summaryRows);
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
            if (MicroCaseStage.INCUBATING.name().equals(row.stage)) {
                summary.incubating++;
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

    private MicroWorklistQueryForm normalize(MicroWorklistQueryForm query) {
        MicroWorklistQueryForm normalized = new MicroWorklistQueryForm();
        if (query == null) {
            return normalized;
        }
        normalized.workflow = text(query.workflow);
        normalized.stage = text(query.stage);
        normalized.urgency = text(query.urgency);
        normalized.due = text(query.due);
        normalized.q = text(query.q);
        normalized.sort = query.sort != null && List.of("priority", "newest", "workflow").contains(query.sort)
                ? query.sort
                : "priority";
        normalized.page = Math.max(1, query.page);
        normalized.pageSize = Math.max(1, Math.min(100, query.pageSize));
        return normalized;
    }

    private boolean matches(MicroWorklistRowForm row, MicroWorklistQueryForm query) {
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
        String searchable = String.join(" ", safe(row.caseId), safe(row.sampleItemId), safe(row.workflowType),
                safe(row.stage), safe(row.dueAction), safe(row.urgency)).toLowerCase(Locale.ROOT);
        return searchable.contains(query.q.toLowerCase(Locale.ROOT));
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private MicroWorklistRowForm toRow(MicroCase microCase) {
        List<MicroIsolate> isolates = isolateDAO.getByCaseId(microCase.getId());
        List<MicroCriticalCommunication> communications = communicationDAO.getByCaseId(microCase.getId());
        AstQueueState astState = astState(isolates);
        MicroWorklistRowForm row = new MicroWorklistRowForm();
        row.caseId = microCase.getId();
        row.sampleItemId = microCase.getSampleItemId();
        row.workflowType = microCase.getWorkflowType();
        row.stage = microCase.getStage();
        row.priority = microCase.getPriority();
        row.createdAt = microCase.getCreatedAt();
        row.needsAstReview = astState.needsReview();
        row.hasOpenCriticalCommunication = hasOpenCriticalCommunication(communications);
        row.dueAction = dueAction(microCase, isolates, astState);
        row.urgency = urgency(microCase, row.needsAstReview, row.hasOpenCriticalCommunication);
        for (MicroCase sibling : caseDAO.getBySampleItem(microCase.getSampleItemId())) {
            if (!sibling.getId().equals(microCase.getId())) {
                row.siblingWorkflows.add(sibling.getWorkflowType());
            }
        }
        return row;
    }

    private AstQueueState astState(List<MicroIsolate> isolates) {
        boolean needsReview = false;
        boolean significantIsolateNeedsEntry = false;
        for (MicroIsolate isolate : isolates) {
            List<MicroAstRun> runs = astRunDAO.getByIsolateId(isolate.getId());
            if (runs.isEmpty()
                    && MicroIsolateSignificance.CLINICALLY_SIGNIFICANT.name().equals(isolate.getSignificance())) {
                significantIsolateNeedsEntry = true;
            }
            for (MicroAstRun run : runs) {
                if (!MicroAstRunStatus.REVIEWED.name().equals(run.getStatus())) {
                    needsReview = true;
                }
            }
        }
        return new AstQueueState(needsReview, significantIsolateNeedsEntry);
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

    private String dueAction(MicroCase microCase, List<MicroIsolate> isolates, AstQueueState astState) {
        if (astState.needsReview()) {
            return "AST_REVIEW";
        }
        if (MicroCaseStage.RECEIVED.name().equals(microCase.getStage())) {
            return "SETUP";
        }
        if (isolates.isEmpty()) {
            return "ISOLATE_ID";
        }
        if (astState.significantIsolateNeedsEntry()) {
            return "AST_ENTRY";
        }
        return "CASE_REVIEW";
    }

    private record AstQueueState(boolean needsReview, boolean significantIsolateNeedsEntry) {
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
        if ("AST_REVIEW".equals(row.dueAction)) {
            return 0;
        }
        if ("SETUP".equals(row.dueAction)) {
            return 1;
        }
        if ("ISOLATE_ID".equals(row.dueAction)) {
            return 2;
        }
        if ("AST_ENTRY".equals(row.dueAction)) {
            return 3;
        }
        return 4;
    }
}
