package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.MicroCaseRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MicroBenchServiceImpl implements MicroBenchService {
    private final MicroCaseWorkspaceService workspace;
    private final MicroCultureService cultures;
    private final MicroIncomingItemDAO incoming;
    private final MicroCaseAnalysisDAO analyses;
    private final MicroCaseOrderDetailDAO details;
    private final MicroCaseReadinessService readiness;
    private final MicrobiologyCaseAccessService access;
    private final MicroBenchCaseService writes;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.referral.dao.ReferralDAO referrals;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroCaseInoculationDAO inoculations;

    public MicroBenchServiceImpl(MicroCaseWorkspaceService workspace, MicroCultureService cultures,
            MicroIncomingItemDAO incoming, MicroCaseAnalysisDAO analyses, MicroCaseOrderDetailDAO details,
            MicroCaseReadinessService readiness, MicrobiologyCaseAccessService access, MicroBenchCaseService writes) {
        this.workspace = workspace;
        this.cultures = cultures;
        this.incoming = incoming;
        this.analyses = analyses;
        this.details = details;
        this.readiness = readiness;
        this.access = access;
        this.writes = writes;
    }

    @Transactional(readOnly = true)
    public MicroBenchPageForm search(MicroBenchQueryForm query, String actor) {
        var result = new MicroBenchPageForm();
        result.page = Math.max(1, query.page);
        result.pageSize = Math.max(1, Math.min(100, query.pageSize));
        var q = new MicroCaseSearchForm();
        q.q = query.q;
        q.labUnitId = query.labUnitId;
        q.status = query.status;
        q.from = query.from;
        q.to = query.to;
        q.pageSize = 100;
        q.page = 1;
        var rows = new ArrayList<MicroBenchPageForm.Row>();
        while (true) {
            var page = workspace.search(q, actor);
            result.labUnits = page.labUnits;
            for (var c : page.rows) {
                if (query.labUnitIds != null && !query.labUnitIds.isEmpty() && !query.labUnitIds.contains(c.labUnitId))
                    continue;
                var cultureRows = cultures.getRows(c.id).stream().filter(r -> !r.receivedIsolate).toList();
                var base = new ArrayList<String>();
                var queued = incoming.forCase(c.id);
                if (queued.stream().anyMatch(i -> java.util.Set.of("WAITING", "FAILED").contains(i.getState())))
                    base.add("INCOMING");
                if (queued.stream().anyMatch(i -> "NEEDS_AMENDMENT".equals(i.getState())))
                    base.add("NEEDS_AMENDMENT");
                if (queued.stream().anyMatch(i -> "REFERRAL".equals(i.getSource()) && !"PLACED".equals(i.getState())))
                    base.add("REFERRAL_RETURNED");
                var sourceIds = inoculations.getSources(c.id).stream().map(s -> s.getId()).toList();
                if (!sourceIds.isEmpty()
                        && referrals.getWorkspaceReferrals(sourceIds).stream().anyMatch(
                                r -> r.getStatus() == org.openelisglobal.referral.valueholder.ReferralStatus.COMPLETED
                                        && !Boolean.TRUE.equals(r.getReconciled()))
                        && !base.contains("REFERRAL_RETURNED"))
                    base.add("REFERRAL_RETURNED");
                var required = readiness.getReadiness(c.id);
                if (required.blockers.stream().anyMatch(b -> b.contains("CLINICAL_FIELD_REQUIRED")
                        || b.contains("ORIGIN") || b.contains("PROGRAM") || b.contains("QUESTION")))
                    base.add("REQUIRED_FIELDS");
                if ("AWAITING_INOCULATION".equals(query.cultureFilter)) {
                    if (cultureRows.isEmpty() && !cultureTests(c.id).isEmpty())
                        rows.add(row(c, null, base, actor));
                } else if ("CHECK_DUE".equals(query.cultureFilter) || "FINAL_READ_DUE".equals(query.cultureFilter)) {
                    for (var culture : cultureRows)
                        if ("CHECK_DUE".equals(query.cultureFilter) ? culture.checkDue : culture.finalReadDue)
                            rows.add(row(c, culture, base, actor));
                } else {
                    var r = row(c, null, base, actor);
                    for (var culture : cultureRows)
                        attention(r, culture);
                    rows.add(r);
                }
            }
            if ((long) q.page * q.pageSize >= page.total)
                break;
            q.page++;
        }
        rows.removeIf(r -> query.needsAttention && r.attention.isEmpty());
        rows.sort(Comparator.comparingInt((MicroBenchPageForm.Row r) -> "STAT".equals(r.priority) ? 0 : 1)
                .thenComparing(r -> r.dueAt, Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(r -> r.id));
        result.total = rows.size();
        int from = Math.min(rows.size(), (result.page - 1) * result.pageSize);
        result.rows = rows.subList(from, Math.min(rows.size(), from + result.pageSize));
        return result;
    }

    private MicroBenchPageForm.Row row(MicroCaseSummaryForm c, MicroCultureForm culture, List<String> base,
            String actor) {
        var r = new MicroBenchPageForm.Row();
        r.id = culture == null ? c.id : c.id + ":" + culture.id;
        r.caseId = c.id;
        r.cultureId = culture == null ? null : culture.id;
        r.accessionNumber = c.accessionNumber;
        r.patientName = c.patientName;
        r.labUnitId = c.labUnitId;
        r.labUnit = c.labUnit;
        r.specimenType = c.specimenType;
        r.priority = c.priority;
        r.stage = c.stage;
        r.culture = culture;
        r.attention.addAll(base);
        r.canWrite = access.hasLabUnitRole(actor, c.labUnitId, Constants.ROLE_RESULTS);
        var orderDetail = details.getByCaseId(c.id);
        r.origin = orderDetail == null ? null : orderDetail.getPatientOrigin();
        var shell = workspace.get(c.id, actor);
        r.canWrite = shell.canWrite;
        r.cultureTests = cultureTests(c.id);
        if (!shell.samples.isEmpty()) {
            r.sampleItemId = shell.samples.get(0).sampleItemId;
            r.sampleTypeId = shell.samples.get(0).sampleTypeId;
        }
        if (culture != null) {
            r.sampleItemId = culture.sourceSampleItemId;
            attention(r, culture);
        }
        return r;
    }

    private List<IdValuePair> cultureTests(String id) {
        return analyses.getAnalyses(id).stream()
                .filter(a -> "CULTURE".equals(a.getTest().getMicrobiologyCaseRole())
                        || analyses.getByCaseAndAnalysis(id, a.getId()).getCaseRole() == MicroCaseRole.CULTURE)
                .map(a -> new IdValuePair(a.getTest().getId(), a.getTest().getName())).distinct().toList();
    }

    private void attention(MicroBenchPageForm.Row r, MicroCultureForm c) {
        if (c.checkDue && !r.attention.contains("CHECK_DUE"))
            r.attention.add("CHECK_DUE");
        if (c.finalReadDue && !r.attention.contains("INCUBATION_COMPLETE"))
            r.attention.add("INCUBATION_COMPLETE");
        if (c.proposals.stream().anyMatch(p -> p.confirmedAt() == null) && !r.attention.contains("NEGATIVE_CONFIRM"))
            r.attention.add("NEGATIVE_CONFIRM");
        if (c.outcome == null || "GROWTH".equals(c.outcome))
            for (var time : Arrays.asList(c.nextCheck, c.incubationEnds))
                if (time != null && (r.dueAt == null || time.before(r.dueAt)))
                    r.dueAt = time;
    }

    public List<MicroBenchResultForm> apply(MicroBenchRequestForm request, String actor) {
        if (request == null || request.cases == null || request.cases.isEmpty() || request.cases.size() > 100
                || !List.of("NO_GROWTH", "INOCULATE", "EXTEND").contains(request.action == null ? "" : request.action))
            throw new IllegalArgumentException("MICROBIOLOGY_BENCH_SELECTION");
        Set<String> ids = new HashSet<>();
        for (var selection : request.cases)
            if (selection == null || selection.caseId == null || !ids.add(selection.caseId))
                throw new IllegalArgumentException("MICROBIOLOGY_BENCH_SELECTION");
        List<MicroBenchResultForm> replies = new ArrayList<>();
        for (var selection : request.cases) {
            try {
                replies.add(writes.apply(request, selection, actor));
            } catch (RuntimeException failure) {
                org.openelisglobal.common.log.LogEvent.logError(failure);
                var r = new MicroBenchResultForm();
                r.caseId = selection.caseId;
                r.error = failure instanceof org.springframework.security.access.AccessDeniedException
                        ? "MICROBIOLOGY_BENCH_ACCESS"
                        : failure instanceof MicroCaseLockedException ? "MICROBIOLOGY_BENCH_LOCKED"
                                : "MICROBIOLOGY_BENCH_INVALID";
                replies.add(r);
            }
        }
        return replies;
    }
}
