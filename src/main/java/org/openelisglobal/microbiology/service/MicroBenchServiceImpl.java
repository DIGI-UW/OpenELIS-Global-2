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
    private final MicrobiologyCaseAccessService access;
    private final MicroBenchCaseService writes;
    @org.springframework.beans.factory.annotation.Autowired
    private org.openelisglobal.referral.dao.ReferralDAO referrals;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroCaseInoculationDAO inoculations;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroCaseSearchDAO search;
    @org.springframework.beans.factory.annotation.Autowired
    private MicroCaseFinalRequirementsService requirements;

    public MicroBenchServiceImpl(MicroCaseWorkspaceService workspace, MicroCultureService cultures,
            MicroIncomingItemDAO incoming, MicroCaseAnalysisDAO analyses, MicroCaseOrderDetailDAO details,
            MicrobiologyCaseAccessService access, MicroBenchCaseService writes) {
        this.workspace = workspace;
        this.cultures = cultures;
        this.incoming = incoming;
        this.analyses = analyses;
        this.details = details;
        this.access = access;
        this.writes = writes;
    }

    @Transactional(readOnly = true)
    public MicroBenchPageForm search(MicroBenchQueryForm query, String actor) {
        var result = new MicroBenchPageForm();
        result.page = Math.max(1, query.page);
        result.pageSize = Math.max(1, Math.min(100, query.pageSize));
        // Normalize filters and enforce the same unit boundary as the case worklist.
        result.labUnits = workspace.search(query, actor).labUnits;
        Set<String> resultsUnits = access.permittedLabUnitIds(actor, Constants.ROLE_RESULTS);
        Set<String> validationUnits = access.permittedLabUnitIds(actor, Constants.ROLE_VALIDATION);
        Set<String> allowed = null;
        if (resultsUnits != null && validationUnits != null) {
            allowed = new HashSet<>(resultsUnits);
            allowed.addAll(validationUnits);
        }
        if (query.labUnitIds != null && !query.labUnitIds.isEmpty()) {
            if (allowed == null)
                allowed = new HashSet<>(query.labUnitIds);
            else
                allowed.retainAll(query.labUnitIds);
        }
        if (query.cultureFilter != null
                && !Set.of("", "AWAITING_INOCULATION", "CHECK_DUE", "FINAL_READ_DUE").contains(query.cultureFilter))
            throw new IllegalArgumentException("MICROBIOLOGY_BENCH_FILTER");
        // Conditional FHIR questionnaire answers cannot be evaluated in HQL. Only
        // the explicit attention filter needs those candidates; ordinary pages do not.
        List<String> questionnaireIds = query.needsAttention
                ? search.questionnaireCases(query, allowed).stream()
                        .filter(id -> requirements.blockers(id).stream()
                                .anyMatch(reason -> reason.contains("QUESTION")))
                        .toList()
                : List.of();
        double now = System.currentTimeMillis() / 1000.0;
        result.total = search.benchCount(query, allowed, requirements.requiredFields(), questionnaireIds, now);
        var selected = search.benchPage(query, allowed, requirements.requiredFields(), questionnaireIds, now);
        var shells = new HashMap<String, MicroCaseShellForm>();
        var culturesByCase = new HashMap<String, List<MicroCultureForm>>();
        var attentionByCase = new HashMap<String, List<String>>();
        var rows = new ArrayList<MicroBenchPageForm.Row>();
        boolean cultureGrain = "CHECK_DUE".equals(query.cultureFilter) || "FINAL_READ_DUE".equals(query.cultureFilter);
        for (var selection : selected) {
            String caseId = (String) selection[0];
            var shell = shells.computeIfAbsent(caseId, id -> workspace.get(id, actor));
            var cultureRows = culturesByCase.computeIfAbsent(caseId,
                    id -> cultures.getRows(id).stream().filter(r -> !r.receivedIsolate).toList());
            var base = attentionByCase.computeIfAbsent(caseId, this::caseAttention);
            var culture = cultureGrain
                    ? cultureRows.stream().filter(r -> r.id.equals(selection[1])).findFirst().orElse(null)
                    : null;
            var row = row(shell, culture, base, actor);
            if (!cultureGrain)
                for (var item : cultureRows)
                    attention(row, item);
            rows.add(row);
        }
        result.rows = rows;
        return result;
    }

    private List<String> caseAttention(String caseId) {
        var base = new ArrayList<String>();
        var queued = incoming.forCase(caseId);
        if (queued.stream().anyMatch(i -> Set.of("WAITING", "FAILED").contains(i.getState())))
            base.add("INCOMING");
        if (queued.stream().anyMatch(i -> "NEEDS_AMENDMENT".equals(i.getState())))
            base.add("NEEDS_AMENDMENT");
        if (queued.stream().anyMatch(i -> "REFERRAL".equals(i.getSource()) && !"PLACED".equals(i.getState())))
            base.add("REFERRAL_RETURNED");
        var sourceIds = inoculations.getSources(caseId).stream().map(s -> s.getId()).toList();
        if (!sourceIds.isEmpty() && referrals.getWorkspaceReferrals(sourceIds).stream()
                .anyMatch(r -> r.getStatus() == org.openelisglobal.referral.valueholder.ReferralStatus.COMPLETED
                        && !Boolean.TRUE.equals(r.getReconciled()))
                && !base.contains("REFERRAL_RETURNED"))
            base.add("REFERRAL_RETURNED");
        if (!requirements.blockers(caseId).isEmpty())
            base.add("REQUIRED_FIELDS");
        return base;
    }

    private MicroBenchPageForm.Row row(MicroCaseShellForm c, MicroCultureForm culture, List<String> base,
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
        var shell = c;
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
