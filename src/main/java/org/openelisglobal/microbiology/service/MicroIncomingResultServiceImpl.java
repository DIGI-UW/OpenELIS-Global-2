package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.*;
import org.openelisglobal.microbiology.valueholder.*;
import org.openelisglobal.test.service.TestService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@ManagesCaseLocks
@Transactional(readOnly = true)
public class MicroIncomingResultServiceImpl implements MicroIncomingResultService {
    private final MicroIncomingItemDAO incoming;
    private final MicroCaseDAO cases;
    private final MicroCaseAnalysisDAO links;
    private final MicroCaseActivityDAO activities;
    private final MicrobiologyCaseAccessService access;
    private final MicroCaseResultService results;
    private final TestService tests;
    private final org.openelisglobal.analysis.service.AnalysisService analyses;
    private final ObjectMapper json;

    @org.springframework.beans.factory.annotation.Autowired
    public MicroIncomingResultServiceImpl(MicroIncomingItemDAO incoming, MicroCaseDAO cases, MicroCaseAnalysisDAO links,
            MicroCaseActivityDAO activities, MicrobiologyCaseAccessService access, MicroCaseResultService results,
            TestService tests, org.openelisglobal.analysis.service.AnalysisService analyses) {
        this(incoming, cases, links, activities, access, results, tests, analyses, new ObjectMapper());
    }

    public MicroIncomingResultServiceImpl(MicroIncomingItemDAO incoming, MicroCaseDAO cases, MicroCaseAnalysisDAO links,
            MicroCaseActivityDAO activities, MicrobiologyCaseAccessService access, MicroCaseResultService results,
            TestService tests, org.openelisglobal.analysis.service.AnalysisService analyses, ObjectMapper json) {
        this.incoming = incoming;
        this.cases = cases;
        this.links = links;
        this.activities = activities;
        this.access = access;
        this.results = results;
        this.tests = tests;
        this.analyses = analyses;
        this.json = json;
    }

    public List<MicroIncomingForm> list(String caseId, String actor) {
        if (actor == null || actor.isBlank())
            throw new AccessDeniedException("Authenticated user required");
        return incoming.forCase(caseId).stream().map(this::form).toList();
    }

    @Transactional
    public MicroIncomingForm receive(String caseId, String sampleItemId, String testId, String source, String key,
            String raw, MicroCaseResultRequestForm result, boolean exactPlacement, String actor) {
        return receive(caseId, sampleItemId, testId, source, key, raw, result, exactPlacement, actor, null);
    }

    @Transactional
    public MicroIncomingForm receive(String caseId, String sampleItemId, String testId, String source, String key,
            String raw, MicroCaseResultRequestForm result, boolean exactPlacement, String actor, String analyzerId) {
        return receive(caseId, sampleItemId, testId, source, key, raw, result, exactPlacement, actor, analyzerId, null);
    }

    @Transactional
    public MicroIncomingForm receiveReviewed(String caseId, String sampleItemId, String testId, String source,
            String key, String raw, MicroCaseResultRequestForm result, String analysisId, String actor,
            String analyzerId) {
        var c = cases.getForUpdate(caseId);
        if (c == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS))
            throw new AccessDeniedException("Case lab unit Results rights required");
        return receive(caseId, sampleItemId, testId, source, key, raw, result, false, actor, analyzerId, analysisId);
    }

    private MicroIncomingForm receive(String caseId, String sampleItemId, String testId, String source, String key,
            String raw, MicroCaseResultRequestForm result, boolean exactPlacement, String actor, String analyzerId,
            String reviewedAnalysisId) {
        if (!List.of("ANALYZER", "REFERRAL").contains(source) || key == null || key.isBlank() || key.length() > 500
                || result == null || result.components == null || result.components.isEmpty()
                || tests.get(testId) == null)
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_REQUIRED");
        var c = cases.getForUpdate(caseId);
        if (c == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        var prior = incoming.findDelivery(caseId, testId, key);
        if (prior != null)
            return form(prior);
        var item = new MicroIncomingItem();
        item.setMicroCase(c);
        item.setOriginalCaseId(caseId);
        item.setSource(source);
        item.setTestId(testId);
        item.setSampleItemId(sampleItemId);
        item.setRunSourceKey(key);
        item.setRawResultValue(raw);
        var stored = new StoredResult();
        stored.reviewedAnalysisId = reviewedAnalysisId;
        stored.version = result.version;
        stored.note = result.note;
        stored.components = result.components;
        stored.analyzerId = "ANALYZER".equals(source) ? analyzerId : null;
        item.setStructuredResult(encode(stored));
        item.setReceivedAt(Timestamp.from(Instant.now()));
        item.setState("WAITING");
        item.setSysUserId(actor);
        try {
            MicroCaseMutationGuard.requireMutable(c);
        } catch (MicroCaseLockedException locked) {
            item.setState("TERMINAL_CASE_LOCKED".equals(locked.getMessage()) ? "FAILED" : "NEEDS_AMENDMENT");
            if ("FAILED".equals(item.getState()))
                item.setFailureReason("TERMINAL_CASE_LOCKED");
        }
        var matches = links.getAnalyses(caseId).stream().filter(
                a -> testId.equals(a.getTest().getId()) && Objects.equals(sampleItemId, a.getSampleItem().getId()))
                .toList();
        if (reviewedAnalysisId != null) {
            var selected = matches.stream().filter(a -> reviewedAnalysisId.equals(a.getId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("MICROBIOLOGY_INCOMING_AMBIGUOUS"));
            if ("WAITING".equals(item.getState())) {
                var analysis = retainAnalyzer(selected, stored.analyzerId, actor);
                result.version = MicroCaseResultServiceImpl.version(analysis,
                        links.getByCaseAndAnalysis(caseId, reviewedAnalysisId));
                results.saveResults(caseId, reviewedAnalysisId, result, actor, null);
                markPlaced(item, reviewedAnalysisId, actor);
            }
        }
        // Unreviewed delivery can only attach an exact, unambiguous first result.
        else if (exactPlacement && "WAITING".equals(item.getState()) && matches.size() == 1) {
            retainAnalyzer(matches.get(0), stored.analyzerId, actor);
            results.receiveResult(caseId, matches.get(0).getId(), result, actor);
            markPlaced(item, matches.get(0).getId(), actor);
        }
        incoming.insert(item);
        audit(caseId, "INCOMING_RECEIVED", item.getId(), null, actor);
        return form(item);
    }

    @Transactional
    public MicroIncomingForm place(String caseId, String id, MicroIncomingPlacementForm request, String actor,
            HttpServletRequest http) {
        if (request == null || request.sampleItemId == null)
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_REQUIRED");
        String target = request.targetCaseId == null ? caseId : request.targetCaseId;
        // Stable order prevents two opposite moves from deadlocking.
        new TreeSet<>(List.of(caseId, target)).forEach(key -> writeCase(key, actor));
        var item = incoming.withCase(id);
        if (item == null || !caseId.equals(item.getMicroCase().getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if ("PLACED".equals(item.getState()))
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_ALREADY_PLACED");
        boolean moved = !target.equals(caseId) || !Objects.equals(item.getSampleItemId(), request.sampleItemId);
        if (moved && (request.reason == null || request.reason.isBlank() || request.reason.length() > 4000))
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_MOVE_REASON");
        var payload = decode(item.getStructuredResult());
        String reviewedAnalysisId = !moved ? payload.reviewedAnalysisId : null;
        if (reviewedAnalysisId == null) {
            var add = new MicroCaseAddTestsForm();
            add.placement = "ADDITIONAL";
            add.sampleItemId = request.sampleItemId;
            add.testIds = List.of(item.getTestId());
            results.addTests(target, add, actor);
        }
        var candidates = links.getAnalyses(target).stream()
                .filter(a -> item.getTestId().equals(a.getTest().getId())
                        && request.sampleItemId.equals(a.getSampleItem().getId())
                        && (reviewedAnalysisId == null || reviewedAnalysisId.equals(a.getId())))
                .toList();
        if (candidates.size() != 1)
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_AMBIGUOUS");
        String analysisId = candidates.get(0).getId();
        if (reviewedAnalysisId == null && results.getTests(target, actor).stream()
                .filter(t -> analysisId.equals(t.analysisId)).anyMatch(t -> !"NotStarted".equals(t.status)))
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_EXISTING_RESULT");
        if (request.result != null && request.result.components != null) {
            for (var confirmation : request.result.components) {
                if (confirmation == null)
                    continue;
                payload.components.stream()
                        .filter(component -> Objects.equals(component.componentId, confirmation.componentId))
                        .forEach(component -> {
                            component.criticalAcknowledged = confirmation.criticalAcknowledged;
                            component.invalidResultConfirmed = confirmation.invalidResultConfirmed;
                        });
            }
        }
        var analysis = retainAnalyzer(candidates.get(0), payload.analyzerId, actor);
        payload.version = MicroCaseResultServiceImpl.version(analysis, links.getByCaseAndAnalysis(target, analysisId));
        results.saveResults(target, analysisId, payload, actor, http);
        item.setMicroCase(cases.get(target).orElseThrow());
        item.setSampleItemId(request.sampleItemId);
        markPlaced(item, analysisId, actor);
        item.setSysUserId(actor);
        incoming.update(item);
        audit(caseId, moved ? "INCOMING_MOVED" : "INCOMING_PLACED", id, request.reason, actor);
        if (!target.equals(caseId))
            audit(target, "INCOMING_PLACED", id, request.reason, actor);
        return form(item);
    }

    @Transactional
    public MicroIncomingForm fail(String caseId, String id, String reason, String actor) {
        writeCase(caseId, actor);
        if (reason == null || reason.isBlank() || reason.length() > 4000)
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_MOVE_REASON");
        var item = incoming.withCase(id);
        if (item == null || !caseId.equals(item.getMicroCase().getId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if ("PLACED".equals(item.getState()))
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_ALREADY_PLACED");
        item.setState("FAILED");
        item.setFailureReason(reason.trim());
        item.setSysUserId(actor);
        incoming.update(item);
        audit(caseId, "INCOMING_FAILED", id, reason.trim(), actor);
        return form(item);
    }

    private void writeCase(String id, String actor) {
        var c = cases.getForUpdate(id);
        if (c == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS))
            throw new AccessDeniedException("Case lab unit Results rights required");
        MicroCaseMutationGuard.requireMutable(c);
    }

    private void markPlaced(MicroIncomingItem i, String analysis, String actor) {
        i.setState("PLACED");
        i.setPlacedBy(actor);
        i.setPlacedAt(Timestamp.from(Instant.now()));
        i.setPlacementTarget(analysis);
        i.setFailureReason(null);
    }

    private void audit(String caseId, String type, String item, String reason, String actor) {
        var event = new MicroCaseActivity();
        event.setCaseId(caseId);
        event.setActivityType(type);
        event.setPerformedBy(actor);
        event.setSysUserId(actor);
        event.setNote(reason);
        event.setStructuredData(encode(Map.of("incomingId", item)));
        activities.insert(event);
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("MICROBIOLOGY_INCOMING_PAYLOAD", e);
        }
    }

    // Only trusted ingestion supplies this metadata. Placement forms carry result
    // acknowledgements, never a replacement instrument identity.
    private static class StoredResult extends MicroCaseResultRequestForm {
        public String analyzerId;
        public String reviewedAnalysisId;
    }

    private org.openelisglobal.analysis.valueholder.Analysis retainAnalyzer(
            org.openelisglobal.analysis.valueholder.Analysis analysis, String analyzerId, String actor) {
        if (analyzerId == null)
            return analysis;
        analysis.setAnalyzerId(analyzerId);
        analysis.setSysUserId(actor);
        return analyses.update(analysis);
    }

    private StoredResult decode(String value) {
        try {
            return json.readValue(value, StoredResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Stored incoming payload is invalid", e);
        }
    }

    private MicroIncomingForm form(MicroIncomingItem i) {
        var f = new MicroIncomingForm();
        f.id = i.getId();
        f.caseId = i.getMicroCase().getId();
        f.source = i.getSource();
        f.testId = i.getTestId();
        var test = tests.get(i.getTestId());
        f.testName = test == null ? i.getTestId() : test.getName();
        f.sampleItemId = i.getSampleItemId();
        f.state = i.getState();
        f.runSourceKey = i.getRunSourceKey();
        f.rawResultValue = i.getRawResultValue();
        f.receivedAt = i.getReceivedAt();
        f.placedAt = i.getPlacedAt();
        f.placedBy = i.getPlacedBy();
        f.placementTarget = i.getPlacementTarget();
        f.failureReason = i.getFailureReason();
        var stored = decode(i.getStructuredResult());
        f.result = new MicroCaseResultRequestForm();
        f.result.version = stored.version;
        f.result.note = stored.note;
        f.result.components = stored.components;
        return f;
    }
}
