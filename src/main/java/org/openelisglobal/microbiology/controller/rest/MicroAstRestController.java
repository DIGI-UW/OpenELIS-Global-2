package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroAstOverrideRequestForm;
import org.openelisglobal.microbiology.form.MicroAstReadingForm;
import org.openelisglobal.microbiology.form.MicroAstReadingRequestForm;
import org.openelisglobal.microbiology.form.MicroAstRunAntibioticForm;
import org.openelisglobal.microbiology.form.MicroAstRunForm;
import org.openelisglobal.microbiology.form.MicroAstRunRequestForm;
import org.openelisglobal.microbiology.form.MicroAstSetupForm;
import org.openelisglobal.microbiology.service.MicroAstRunSetupCommand;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.valueholder.MicroAstAttemptType;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
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
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroAstRestController extends MicrobiologyRestControllerSupport {

    private final MicroAstService astService;
    private final org.openelisglobal.microbiology.service.MicroAstResponseService responses;

    public MicroAstRestController(MicroAstService astService,
            org.openelisglobal.microbiology.service.MicroAstResponseService responses) {
        this.responses = responses;
        this.astService = astService;
    }

    @GetMapping("/runs")
    public ResponseEntity<List<MicroAstRunForm>> getRunsForIsolate(@RequestParam String isolateId) {
        return ResponseEntity.ok(responses.runs(isolateId));
    }

    @GetMapping("/setup")
    public ResponseEntity<MicroAstSetupForm> getSetup(@RequestParam String isolateId) {
        return ResponseEntity.ok(astService.getSetup(isolateId));
    }

    @GetMapping("/panels/{panelId}/antibiotics")
    public ResponseEntity<List<MicroAstRunAntibioticForm>> getPanelAntibiotics(@PathVariable String panelId) {
        return ResponseEntity.ok(responses.panelAntibiotics(panelId));
    }

    @PostMapping("/runs")
    public ResponseEntity<MicroAstRunForm> startRun(@RequestBody MicroAstRunRequestForm request,
            HttpServletRequest httpRequest) {
        MicroAstRunSetupCommand command = new MicroAstRunSetupCommand(request.isolateId, request.panelId,
                request.breakpointStandardId, request.panelAdjustmentReason, technique(request.technique),
                lotSelections(request.lotSelections), request.orderedAntibioticIds, request.awaitAnalyzerResults,
                request.analyzerInstrumentId, request.analyzerCardId);
        return ResponseEntity.ok(responses.toRunForm(astService.startRun(command, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{runId}/analyzer-flags/acknowledge")
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroAstRunForm> acknowledgeAnalyzerFlags(@PathVariable String runId,
            @RequestBody MicroAstRunRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(responses.toRunFormWithReadings(
                astService.acknowledgeAnalyzerFlags(runId, request.reason, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{runId}/qc/override")
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroAstRunForm> overrideQcFailure(@PathVariable String runId,
            @RequestBody MicroAstRunRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(responses.toRunFormWithReadings(
                astService.overrideQcFailure(runId, request.reason, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{runId}/invalidate-and-repeat")
    public ResponseEntity<MicroAstRunForm> invalidateAndRepeat(@PathVariable String runId,
            @RequestBody MicroAstRunRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(responses.toRunFormWithReadings(astService.invalidateAndRepeat(runId, request.reason,
                request.analyzerCardId, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{sourceRunId}/attempts")
    public ResponseEntity<MicroAstRunForm> startRepeatRun(@PathVariable String sourceRunId,
            @RequestBody MicroAstRunRequestForm request, HttpServletRequest httpRequest) {
        if (request.lotSelections == null || request.lotSelections.isEmpty()) {
            if (request.orderedAntibioticIds != null && !request.orderedAntibioticIds.isEmpty()) {
                return ResponseEntity.ok(responses.toRunForm(astService.startRepeatRun(sourceRunId,
                        MicroAstAttemptType.valueOf(request.attemptType), request.reason, technique(request.technique),
                        List.of(), request.orderedAntibioticIds, authenticatedUserId(httpRequest))));
            }
            return ResponseEntity.ok(responses
                    .toRunForm(astService.startRepeatRun(sourceRunId, MicroAstAttemptType.valueOf(request.attemptType),
                            request.reason, technique(request.technique), authenticatedUserId(httpRequest))));
        }
        return ResponseEntity.ok(responses.toRunForm(astService.startRepeatRun(sourceRunId,
                MicroAstAttemptType.valueOf(request.attemptType), request.reason, technique(request.technique),
                lotSelections(request.lotSelections), request.orderedAntibioticIds, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{runId}/readings")
    public ResponseEntity<MicroAstReadingForm> recordReading(@PathVariable String runId,
            @RequestBody MicroAstReadingRequestForm request, HttpServletRequest httpRequest) {
        MicroAstReading reading = astService.recordReading(runId, request.antibioticId, request.rawValue,
                authenticatedUserId(httpRequest));
        return ResponseEntity.ok(responses.toReadingForm(reading));
    }

    @PutMapping("/readings/{readingId}/override")
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroAstReadingForm> overrideReading(@PathVariable String readingId,
            @RequestBody MicroAstOverrideRequestForm request, HttpServletRequest httpRequest) {
        MicroAstReading reading = astService.overrideReading(readingId,
                requiredEnum(MicroAstInterpretation.class, request.overrideInterpretation, "overrideInterpretation"),
                request.overrideReason, authenticatedUserId(httpRequest));
        return ResponseEntity.ok(responses.toReadingFormWithHistory(reading));
    }

    @PostMapping("/readings/{readingId}/override/revert")
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroAstReadingForm> revertOverride(@PathVariable String readingId,
            @RequestBody MicroAstOverrideRequestForm request, HttpServletRequest httpRequest) {
        MicroAstReading reading = astService.revertOverride(readingId, request.overrideReason,
                authenticatedUserId(httpRequest));
        return ResponseEntity.ok(responses.toReadingFormWithHistory(reading));
    }

    @PostMapping("/runs/{runId}/review")
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroAstRunForm> reviewRun(@PathVariable String runId,
            @RequestBody MicroAstRunRequestForm request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(responses.toRunForm(astService.reviewRun(runId, authenticatedUserId(httpRequest))));
    }

    @PostMapping("/runs/{runId}/reportable")
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<MicroAstRunForm> selectReportableRun(@PathVariable String runId,
            HttpServletRequest httpRequest) {
        return ResponseEntity
                .ok(responses.toRunForm(astService.selectReportableRun(runId, authenticatedUserId(httpRequest))));
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

}
