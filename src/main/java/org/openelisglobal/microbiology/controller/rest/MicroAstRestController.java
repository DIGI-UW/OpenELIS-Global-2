package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroAstOverrideEventForm;
import org.openelisglobal.microbiology.form.MicroAstOverrideRequestForm;
import org.openelisglobal.microbiology.form.MicroAstReadingForm;
import org.openelisglobal.microbiology.form.MicroAstReadingRequestForm;
import org.openelisglobal.microbiology.form.MicroAstRunAntibioticForm;
import org.openelisglobal.microbiology.form.MicroAstRunForm;
import org.openelisglobal.microbiology.form.MicroAstRunRequestForm;
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
import org.openelisglobal.microbiology.valueholder.MicroAstRunAntibiotic;
import org.openelisglobal.microbiology.valueholder.MicroAstTechnique;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/ast")
public class MicroAstRestController extends MicrobiologyRestControllerSupport {

    private final MicroAstService astService;

    public MicroAstRestController(MicroAstService astService) {
        this.astService = astService;
    }

    @GetMapping("/runs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MicroAstRunForm>> getRunsForIsolate(@RequestParam String isolateId) {
        List<MicroAstRunForm> forms = new ArrayList<>();
        for (MicroAstRun run : astService.getRunsForIsolate(isolateId)) {
            forms.add(toRunFormWithReadings(run));
        }
        return ResponseEntity.ok(forms);
    }

    @GetMapping("/setup")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstSetupForm> getSetup(@RequestParam String isolateId) {
        return ResponseEntity.ok(astService.getSetup(isolateId));
    }

    @PostMapping("/runs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstRunForm> startRun(@RequestBody MicroAstRunRequestForm request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toRunForm(astService.startRun(request.isolateId, request.panelId,
                request.breakpointStandardId, request.panelAdjustmentReason, technique(request.technique),
                lotSelections(request.lotSelections), authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{sourceRunId}/attempts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstRunForm> startRepeatRun(@PathVariable String sourceRunId,
            @RequestBody MicroAstRunRequestForm request, HttpServletRequest httpRequest) {
        if (request.lotSelections == null || request.lotSelections.isEmpty()) {
            return ResponseEntity.ok(
                    toRunForm(astService.startRepeatRun(sourceRunId, MicroAstAttemptType.valueOf(request.attemptType),
                            request.reason, technique(request.technique), authenticatedUserId(httpRequest))));
        }
        return ResponseEntity.ok(toRunForm(astService.startRepeatRun(sourceRunId,
                MicroAstAttemptType.valueOf(request.attemptType), request.reason, technique(request.technique),
                lotSelections(request.lotSelections), authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{runId}/readings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstReadingForm> recordReading(@PathVariable String runId,
            @RequestBody MicroAstReadingRequestForm request, HttpServletRequest httpRequest) {
        MicroAstReading reading = astService.recordReading(runId, request.antibioticId, request.rawValue,
                authenticatedUserId(httpRequest));
        return ResponseEntity.ok(toReadingForm(reading));
    }

    @PutMapping("/readings/{readingId}/override")
    @PreAuthorize("hasAnyRole('ADMIN', 'VALIDATION')")
    public ResponseEntity<MicroAstReadingForm> overrideReading(@PathVariable String readingId,
            @RequestBody MicroAstOverrideRequestForm request, HttpServletRequest httpRequest) {
        MicroAstReading reading = astService.overrideReading(readingId,
                MicroAstInterpretation.valueOf(request.overrideInterpretation), request.overrideReason,
                authenticatedUserId(httpRequest));
        return ResponseEntity.ok(toReadingForm(reading, astService.getOverrideHistoryForRun(reading.getAstRunId())));
    }

    @PostMapping("/readings/{readingId}/override/revert")
    @PreAuthorize("hasAnyRole('ADMIN', 'VALIDATION')")
    public ResponseEntity<MicroAstReadingForm> revertOverride(@PathVariable String readingId,
            @RequestBody MicroAstOverrideRequestForm request, HttpServletRequest httpRequest) {
        MicroAstReading reading = astService.revertOverride(readingId, request.overrideReason,
                authenticatedUserId(httpRequest));
        return ResponseEntity.ok(toReadingForm(reading, astService.getOverrideHistoryForRun(reading.getAstRunId())));
    }

    @PostMapping("/runs/{runId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstRunForm> reviewRun(@PathVariable String runId,
            @RequestBody MicroAstRunRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toRunForm(astService.reviewRun(runId, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{runId}/reportable")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstRunForm> selectReportableRun(@PathVariable String runId,
            @RequestBody MicroAstRunRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(toRunForm(astService.selectReportableRun(runId, authenticatedUserId(httpRequest))));
    }

    private MicroAstRunForm toRunFormWithReadings(MicroAstRun run) {
        MicroAstRunForm form = toRunForm(run);
        List<MicroAstOverrideEventForm> overrideHistory = astService.getOverrideHistoryForRun(run.getId());
        for (MicroAstReading reading : astService.getReadingsForRun(run.getId())) {
            form.readings.add(toReadingForm(reading, overrideHistory));
        }
        return form;
    }

    private MicroAstTechnique technique(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AST_TECHNIQUE_REQUIRED");
        }
        MicroAstTechnique technique = MicroAstTechnique.valueOf(value);
        if (technique.isLegacyUnspecified()) {
            throw new IllegalArgumentException("AST_TECHNIQUE_REQUIRED");
        }
        return technique;
    }

    private MicroAstRunForm toRunForm(MicroAstRun run) {
        MicroAstRunForm form = new MicroAstRunForm();
        form.id = run.getId();
        form.isolateId = run.getIsolateId();
        form.panelId = run.getPanelId();
        form.panelVersion = run.getPanelVersion();
        form.panelProvenance = run.getPanelProvenance();
        form.panelAdjustmentReason = run.getPanelAdjustmentReason();
        form.breakpointStandardId = run.getBreakpointStandardId();
        form.breakpointVersion = run.getBreakpointVersion();
        form.attemptType = run.getAttemptType();
        form.sourceRunId = run.getSourceRunId();
        form.attemptReason = run.getAttemptReason();
        form.method = run.getMethod();
        form.technique = run.getTechnique();
        form.measurementType = run.getMethod();
        form.reportable = run.isReportable();
        form.status = run.getStatus();
        form.startedAt = run.getStartedAt();
        form.startedBy = run.getStartedBy();
        form.reviewedAt = run.getReviewedAt();
        form.reviewedBy = run.getReviewedBy();
        for (MicroAstRunAntibiotic ordered : astService.getOrderedAntibioticsForRun(run.getId())) {
            MicroAstRunAntibioticForm orderedForm = new MicroAstRunAntibioticForm();
            orderedForm.antibioticId = ordered.getAntibioticId();
            orderedForm.displayOrder = ordered.getDisplayOrder();
            orderedForm.tier = ordered.getTier();
            orderedForm.reportBehavior = ordered.getReportBehavior();
            form.orderedAntibiotics.add(orderedForm);
        }
        return form;
    }

    private MicroAstReadingForm toReadingForm(MicroAstReading reading) {
        return toReadingForm(reading, List.of());
    }

    private MicroAstReadingForm toReadingForm(MicroAstReading reading,
            List<MicroAstOverrideEventForm> overrideHistory) {
        MicroAstReadingForm form = new MicroAstReadingForm();
        form.id = reading.getId();
        form.astRunId = reading.getAstRunId();
        form.antibioticId = reading.getAntibioticId();
        form.measurementType = reading.getMethod();
        form.method = reading.getMethod();
        form.rawValue = reading.getRawValue();
        form.rawText = reading.getRawText();
        form.interpretation = reading.getInterpretation();
        form.breakpointRuleId = reading.getBreakpointRuleId();
        form.source = reading.getSource();
        form.matchedBy = reading.getMatchedBy();
        form.units = reading.getUnits();
        form.overrideInterpretation = reading.getOverrideInterpretation();
        form.overrideReason = reading.getOverrideReason();
        form.createdAt = reading.getCreatedAt();
        form.createdBy = reading.getCreatedBy();
        form.overrideHistory = overrideHistory.stream().filter(event -> reading.getId().equals(event.readingId))
                .toList();
        return form;
    }
}
