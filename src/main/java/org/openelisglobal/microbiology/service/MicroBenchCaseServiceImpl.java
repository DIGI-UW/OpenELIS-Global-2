package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ManagesCaseLocks
public class MicroBenchCaseServiceImpl implements MicroBenchCaseService {
    private final MicroCaseDAO cases;
    private final MicroCaseInoculationDAO rows;
    private final MicroCaseAnalysisDAO analyses;
    private final MicroBenchActionDAO actions;
    private final MicroCaseActivityDAO activities;
    private final MicroCultureService cultures;
    private final MicrobiologyCaseAccessService access;
    private final ObjectMapper json;

    @org.springframework.beans.factory.annotation.Autowired
    public MicroBenchCaseServiceImpl(MicroCaseDAO cases, MicroCaseInoculationDAO rows, MicroCaseAnalysisDAO analyses,
            MicroBenchActionDAO actions, MicroCaseActivityDAO activities, MicroCultureService cultures,
            MicrobiologyCaseAccessService access) {
        this(cases, rows, analyses, actions, activities, cultures, access, new ObjectMapper());
    }

    public MicroBenchCaseServiceImpl(MicroCaseDAO cases, MicroCaseInoculationDAO rows, MicroCaseAnalysisDAO analyses,
            MicroBenchActionDAO actions, MicroCaseActivityDAO activities, MicroCultureService cultures,
            MicrobiologyCaseAccessService access, ObjectMapper json) {
        this.cases = cases;
        this.rows = rows;
        this.analyses = analyses;
        this.actions = actions;
        this.activities = activities;
        this.cultures = cultures;
        this.access = access;
        this.json = json;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MicroBenchResultForm apply(MicroBenchRequestForm request, MicroBenchRequestForm.Selection selection,
            String actor) {
        var c = writeCase(selection.caseId, actor);
        var reply = new MicroBenchResultForm();
        reply.caseId = c.getId();
        var saved = new MicroBenchAction();
        saved.setCaseId(c.getId());
        saved.setAction(request.action);
        saved.setPerformedBy(actor);
        saved.setPerformedAt(Timestamp.from(Instant.now()));
        saved.setBeforeState(snapshot(c));
        saved.setSysUserId(actor);
        var event = new MicroCaseActivity();
        event.setCaseId(c.getId());
        event.setPerformedBy(actor);
        event.setSysUserId(actor);
        event.setActivityType("BENCH_" + request.action);
        event.setNote(request.culture == null ? null : request.culture.note);
        event.setStructuredData(encode(Map.of("actionId", saved.getId())));
        activities.insert(event);
        var input = json.convertValue(request.culture, MicroCultureRequestForm.class);
        if (input == null)
            input = new MicroCultureRequestForm();
        if ("INOCULATE".equals(request.action)) {
            if (selection.sampleItemId == null || request.sampleTypeId == null || request.cultureTestId == null
                    || analyses.getAnalyses(c.getId()).stream()
                            .noneMatch(a -> request.cultureTestId.equals(a.getTest().getId())
                                    && selection.sampleItemId.equals(a.getSampleItem().getId())
                                    && request.sampleTypeId.equals(a.getSampleItem().getTypeOfSampleId())
                                    && ("CULTURE".equals(a.getTest().getMicrobiologyCaseRole())
                                            || analyses.getByCaseAndAnalysis(c.getId(), a.getId())
                                                    .getCaseRole() == MicroCaseRole.CULTURE)))
                throw new IllegalArgumentException("MICROBIOLOGY_BENCH_SAME_TEST_SAMPLE");
            var before = cultures.getRows(c.getId()).stream().map(r -> r.id).toList();
            input.sourceSampleItemId = selection.sampleItemId;
            input.parentId = null;
            input.containerIdentifier = "PLT-" + c.getId() + "-" + UUID.randomUUID();
            var created = cultures.benchInoculate(c.getId(), input, actor, event).stream()
                    .filter(r -> !before.contains(r.id)).findFirst().orElseThrow();
            var entity = rows.get(created.id).orElseThrow();
            entity.setContainerIdentifier("PLT-" + c.getId() + "-" + created.id);
            rows.update(entity);
            reply.changed.add(created.id);
        } else {
            if (selection.cultureIds == null || selection.cultureIds.isEmpty() || selection.cultureIds.size() > 1000)
                throw new IllegalArgumentException("MICROBIOLOGY_BENCH_SELECTION");
            for (String id : new LinkedHashSet<>(selection.cultureIds)) {
                var row = cultures.getRows(c.getId()).stream().filter(r -> r.id.equals(id)).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_BENCH_SELECTION"));
                if (row.receivedIsolate
                        || (row.outcome != null && !("EXTEND".equals(request.action) && "GROWTH".equals(row.outcome)))
                        || ("NO_GROWTH".equals(request.action) && row.positiveAt != null)) {
                    reply.skipped.add(new MicroBenchResultForm.Skip(id, row.containerIdentifier,
                            row.receivedIsolate ? "RECEIVED_ISOLATE" : row.outcome == null ? "GROWTH" : row.outcome));
                    continue;
                }
                if ("NO_GROWTH".equals(request.action)) {
                    input.outcome = "NO_GROWTH";
                    input.proposalId = null;
                    cultures.benchAct(c.getId(), id, "outcome", input, actor, event);
                } else if ("EXTEND".equals(request.action)) {
                    input.extendBy = new BigDecimal("24");
                    input.unit = "HOURS";
                    cultures.benchAct(c.getId(), id, "extension", input, actor, event);
                } else
                    throw new IllegalArgumentException("MICROBIOLOGY_BENCH_ACTION");
                reply.changed.add(id);
            }
        }
        saved.setAfterState(snapshot(c));
        actions.insert(saved);
        reply.actionId = saved.getId();
        event.setStructuredData(encode(reply));
        activities.update(event);
        return reply;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MicroBenchResultForm undo(String caseId, String actionId, String actor) {
        var c = writeCase(caseId, actor);
        var action = actions.get(actionId).orElseThrow();
        if (!caseId.equals(action.getCaseId()) || action.getUndoneAt() != null
                || !sameSnapshot(snapshot(c), action.getAfterState()))
            throw new IllegalArgumentException("MICROBIOLOGY_BENCH_UNDO_CHANGED");
        try {
            var before = json.readTree(action.getBeforeState());
            Map<String, com.fasterxml.jackson.databind.JsonNode> originals = new HashMap<>();
            before.get("rows").forEach(r -> originals.put(r.get("id").asText(), r));
            for (var row : rows.getRows(caseId)) {
                var old = originals.get(row.getId());
                if (old == null)
                    row.setCancelledAt(Timestamp.from(Instant.now()));
                else {
                    row.setOutcome(text(old, "outcome"));
                    row.setOutcomeBy(text(old, "outcomeBy"));
                    row.setOutcomeAt(old.path("outcomeAt").isNull() ? null
                            : json.treeToValue(old.get("outcomeAt"), Timestamp.class));
                    Set<String> extensionIds = new HashSet<>();
                    old.get("extensions").forEach(e -> extensionIds.add(e.get("id").asText()));
                    row.getExtensions().stream().filter(e -> !extensionIds.contains(e.getId())).forEach(e -> {
                        e.setUndoneAt(Timestamp.from(Instant.now()));
                        e.setSysUserId(actor);
                    });
                }
                row.setSysUserId(actor);
                rows.update(row);
            }
            c.setStage(before.get("stage").asText());
            c.setSysUserId(actor);
            cases.update(c);
            action.setUndoneAt(Timestamp.from(Instant.now()));
            action.setUndoneBy(actor);
            action.setSysUserId(actor);
            actions.update(action);
            var event = new MicroCaseActivity();
            event.setCaseId(caseId);
            event.setActivityType("BENCH_UNDO");
            event.setPerformedBy(actor);
            event.setSysUserId(actor);
            event.setStructuredData(encode(Map.of("actionId", actionId)));
            activities.insert(event);
            var response = new MicroBenchResultForm();
            response.caseId = caseId;
            response.actionId = actionId;
            return response;
        } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
            throw new IllegalStateException(invalid);
        }
    }

    private String text(com.fasterxml.jackson.databind.JsonNode node, String key) {
        return node.hasNonNull(key) ? node.get(key).asText() : null;
    }

    private MicroCase writeCase(String id, String actor) {
        var c = cases.getForUpdate(id);
        if (c == null)
            throw new IllegalArgumentException("MICROBIOLOGY_BENCH_SELECTION");
        if (!access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS))
            throw new AccessDeniedException("Case lab unit Results rights required");
        MicroCaseMutationGuard.requireMutable(c);
        return c;
    }

    private String snapshot(MicroCase c) {
        var list = json.valueToTree(cultures.getRows(c.getId()));
        list.forEach(r -> {
            var n = (ObjectNode) r;
            n.remove(List.of("checkDue", "finalReadDue", "overdue"));
        });
        return encode(Map.of("stage", c.getStage(), "rows", list));
    }

    private boolean sameSnapshot(String current, String saved) {
        try {
            return json.readTree(current).equals(json.readTree(saved));
        } catch (com.fasterxml.jackson.core.JsonProcessingException invalid) {
            throw new IllegalStateException(invalid);
        }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
