package org.openelisglobal.microbiology.controller.rest;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroAstOverrideRequestForm;
import org.openelisglobal.microbiology.form.MicroAstReadingForm;
import org.openelisglobal.microbiology.form.MicroAstReadingRequestForm;
import org.openelisglobal.microbiology.form.MicroAstRunForm;
import org.openelisglobal.microbiology.form.MicroAstRunRequestForm;
import org.openelisglobal.microbiology.service.MicroAstService;
import org.openelisglobal.microbiology.valueholder.MicroAstInterpretation;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroAstReading;
import org.openelisglobal.microbiology.valueholder.MicroAstRun;
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
public class MicroAstRestController extends BaseRestController {

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

    @PostMapping("/runs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstRunForm> startRun(@RequestBody MicroAstRunRequestForm request) {
        return ResponseEntity
                .ok(toRunForm(astService.startRun(request.isolateId, request.panelId, request.performedBy)));
    }

    @PostMapping("/runs/{runId}/readings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstReadingForm> recordReading(@PathVariable String runId,
            @RequestBody MicroAstReadingRequestForm request) {
        MicroAstReading reading = astService.recordReading(runId, request.antibioticId,
                MicroAstMethod.valueOf(request.method), request.rawValue, request.performedBy);
        return ResponseEntity.ok(toReadingForm(reading));
    }

    @PutMapping("/readings/{readingId}/override")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstReadingForm> overrideReading(@PathVariable String readingId,
            @RequestBody MicroAstOverrideRequestForm request) {
        MicroAstReading reading = astService.overrideReading(readingId,
                MicroAstInterpretation.valueOf(request.overrideInterpretation), request.overrideReason,
                request.performedBy);
        return ResponseEntity.ok(toReadingForm(reading));
    }

    @PostMapping("/runs/{runId}/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroAstRunForm> reviewRun(@PathVariable String runId,
            @RequestBody MicroAstRunRequestForm request) {
        return ResponseEntity.ok(toRunForm(astService.reviewRun(runId, request.performedBy)));
    }

    private MicroAstRunForm toRunFormWithReadings(MicroAstRun run) {
        MicroAstRunForm form = toRunForm(run);
        for (MicroAstReading reading : astService.getReadingsForRun(run.getId())) {
            form.readings.add(toReadingForm(reading));
        }
        return form;
    }

    private MicroAstRunForm toRunForm(MicroAstRun run) {
        MicroAstRunForm form = new MicroAstRunForm();
        form.id = run.getId();
        form.isolateId = run.getIsolateId();
        form.panelId = run.getPanelId();
        form.status = run.getStatus();
        form.startedAt = run.getStartedAt();
        form.startedBy = run.getStartedBy();
        form.reviewedAt = run.getReviewedAt();
        form.reviewedBy = run.getReviewedBy();
        return form;
    }

    private MicroAstReadingForm toReadingForm(MicroAstReading reading) {
        MicroAstReadingForm form = new MicroAstReadingForm();
        form.id = reading.getId();
        form.astRunId = reading.getAstRunId();
        form.antibioticId = reading.getAntibioticId();
        form.method = reading.getMethod();
        form.rawValue = reading.getRawValue();
        form.rawText = reading.getRawText();
        form.interpretation = reading.getInterpretation();
        form.breakpointRuleId = reading.getBreakpointRuleId();
        form.overrideInterpretation = reading.getOverrideInterpretation();
        form.overrideReason = reading.getOverrideReason();
        form.createdAt = reading.getCreatedAt();
        form.createdBy = reading.getCreatedBy();
        return form;
    }
}
