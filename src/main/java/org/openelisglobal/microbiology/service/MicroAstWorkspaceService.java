package org.openelisglobal.microbiology.service;

import java.util.*;
import org.openelisglobal.common.constants.Constants;
import org.openelisglobal.microbiology.dao.*;
import org.openelisglobal.microbiology.form.MicroAstWorkspaceForm;
import org.openelisglobal.microbiology.valueholder.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroAstWorkspaceService {
    private final MicroCaseDAO cases;
    private final MicroIsolateDAO isolates;
    private final MicroOrganismDAO organisms;
    private final MicroAstPanelDAO panels;
    private final MicroAstRunDAO runs;
    private final MicroAstReadingDAO readings;
    private final MicroAstService ast;
    private final MicroAstResponseService responses;
    private final MicroAstReportChoiceService choices;
    private final MicroAstPanelAssignmentService assignments;
    private final MicroBreakpointService breakpoints;
    private final MicrobiologyCaseAccessService access;
    private final MicroDstService dst;

    public MicroAstWorkspaceService(MicroCaseDAO cases, MicroIsolateDAO isolates, MicroOrganismDAO organisms,
            MicroAstPanelDAO panels, MicroAstRunDAO runs, MicroAstReadingDAO readings, MicroAstService ast,
            MicroAstResponseService responses, MicroAstReportChoiceService choices,
            MicroAstPanelAssignmentService assignments, MicroBreakpointService breakpoints,
            MicrobiologyCaseAccessService access, MicroDstService dst) {
        this.cases = cases;
        this.isolates = isolates;
        this.organisms = organisms;
        this.panels = panels;
        this.runs = runs;
        this.readings = readings;
        this.ast = ast;
        this.responses = responses;
        this.choices = choices;
        this.assignments = assignments;
        this.breakpoints = breakpoints;
        this.access = access;
        this.dst = dst;
    }

    public MicroAstWorkspaceForm get(String caseId, String actor) {
        var c = cases.get(caseId).orElseThrow();
        var form = new MicroAstWorkspaceForm();
        boolean mutable = true;
        try {
            MicroCaseMutationGuard.requireMutable(c);
        } catch (MicroCaseLockedException e) {
            mutable = false;
        }
        form.canEnter = mutable && access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_RESULTS);
        form.canValidate = mutable && access.hasLabUnitRole(actor, c.getLabUnitId(), Constants.ROLE_VALIDATION);
        form.standards = breakpoints.getActiveStandards().stream().filter(s -> "ACTIVE".equals(s.getLifecycleStatus()))
                .map(s -> new MicroAstWorkspaceForm.Option(s.getId(), s.getAuthority() + " " + s.getVersion()))
                .toList();
        form.isolates = isolates.getByCaseId(caseId).stream().filter(i -> i.getCancelledAt() == null
                && i.getOrganismId() != null && "CONFIRMED".equals(i.getIdentificationStatus())).map(i -> {
                    var o = organisms.get(i.getOrganismId()).orElseThrow();
                    var catalog = panels.search("", "ACTIVE", null, "name", 0, 1000).stream().filter(p -> {
                        try {
                            MicroAstPanelAssignmentService.requireCompatible(o, p);
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }).map(p -> new MicroAstWorkspaceForm.Option(p.getId(), p.getName() + " v" + p.getVersionNumber()))
                            .toList();
                    var assigned = assignments.getAssignments(i.getId()).stream()
                            .map(a -> new MicroAstWorkspaceForm.Assignment(a.getId(), a.getPanelId(),
                                    panels.get(a.getPanelId()).orElseThrow().getName(), a.getProvenance()))
                            .toList();
                    var selected = choices.getChoices(i.getId()).stream().map(
                            choice -> new MicroAstWorkspaceForm.Choice(choice.getAntibioticId(), choice.getReadingId()))
                            .toList();
                    return new MicroAstWorkspaceForm.Isolate(i.getId(), i.getIsolateLabel(), o.getDisplayName(),
                            o.getSusceptibilityProtocol(), catalog, assigned, responses.runs(i.getId()), selected);
                }).toList();
        form.dst = dst.evaluate(caseId);
        return form;
    }

    @Transactional
    public MicroAstWorkspaceForm act(String caseId, MicroAstWorkspaceForm.Action request, String actor,
            boolean validation) {
        var isolate = isolates.get(request.isolateId)
                .orElseThrow(() -> new IllegalArgumentException("Isolate not found"));
        if (!caseId.equals(isolate.getCaseId()))
            throw new IllegalArgumentException("Isolate does not belong to case");
        if (request.runId != null && !isolate.getId().equals(runs.get(request.runId).orElseThrow().getIsolateId()))
            throw new IllegalArgumentException("AST run does not belong to isolate");
        if (request.readingId != null) {
            var reading = readings.get(request.readingId).orElseThrow();
            if (!isolate.getId().equals(runs.get(reading.getAstRunId()).orElseThrow().getIsolateId()))
                throw new IllegalArgumentException("AST reading does not belong to isolate");
        }
        if (validation)
            switch (request.action) {
            case "review" -> ast.reviewRun(request.runId, actor);
            case "override" -> ast.overrideReading(request.readingId,
                    MicroAstInterpretation.valueOf(request.interpretation), request.reason, actor);
            case "revert" -> ast.revertOverride(request.readingId, request.reason, actor);
            case "select" ->
                choices.select(isolate.getId(), request.antibioticId, request.readingId, request.reason, actor);
            case "acknowledge" -> ast.acknowledgeAnalyzerFlags(request.runId, request.reason, actor);
            case "qc-override" -> ast.overrideQcFailure(request.runId, request.reason, actor);
            default -> throw new IllegalArgumentException("Unknown validation action");
            }
        else
            switch (request.action) {
            case "assign" -> assignments.assign(isolate.getId(), request.panelId, actor);
            case "start" ->
                ast.startRun(
                        new MicroAstRunSetupCommand(isolate.getId(), request.panelId, request.breakpointStandardId,
                                request.reason, technique(request.technique), List.of(), null, false, null, null),
                        actor);
            case "reading" -> ast.recordReading(request.runId, request.antibioticId, request.rawValue, actor);
            case "repeat", "retest" ->
                ast.startRepeatRun(request.runId, MicroAstAttemptType.valueOf(request.action.toUpperCase(Locale.ROOT)),
                        request.reason, technique(request.technique), actor);
            case "invalidate" -> ast.invalidateAndRepeat(request.runId, request.reason, request.analyzerCardId, actor);
            default -> throw new IllegalArgumentException("Unknown AST action");
            }
        return get(caseId, actor);
    }

    private MicroAstTechnique technique(String value) {
        var t = MicroAstTechnique.valueOf(value);
        if (t.isLegacyUnspecified())
            throw new IllegalArgumentException("AST_TECHNIQUE_REQUIRED");
        return t;
    }
}
