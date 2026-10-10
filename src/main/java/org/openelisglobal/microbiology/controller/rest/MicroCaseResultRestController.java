package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseResultRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseTestForm;
import org.openelisglobal.microbiology.form.MicroCaseTestedElsewhereRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/analyses")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCaseResultRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseResultService resultService;

    public MicroCaseResultRestController(MicroCaseResultService resultService) {
        this.resultService = resultService;
    }

    @PostMapping
    public ResponseEntity<List<MicroCaseTestForm>> addTests(@PathVariable String caseId,
            @RequestBody org.openelisglobal.microbiology.form.MicroCaseAddTestsForm body, HttpServletRequest request) {
        return ResponseEntity.ok(resultService.addTests(caseId, body, authenticatedUserId(request)));
    }

    @PostMapping("/{analysisId}/validate")
    @PreAuthorize(MicrobiologyRestControllerSupport.SUPERVISOR_ACCESS)
    public ResponseEntity<List<MicroCaseTestForm>> validateResult(@PathVariable String caseId,
            @PathVariable String analysisId, @RequestBody MicroCaseResultRequestForm body, HttpServletRequest request) {
        return ResponseEntity
                .ok(resultService.validateResult(caseId, analysisId, body.version, authenticatedUserId(request)));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(org.openelisglobal.microbiology.service.MicroCaseResultAcknowledgementException.class)
    public ResponseEntity<java.util.Map<String, Object>> acknowledgementRequired(
            org.openelisglobal.microbiology.service.MicroCaseResultAcknowledgementException error) {
        return ResponseEntity.unprocessableEntity().body(error.getBody());
    }

    @GetMapping
    public ResponseEntity<List<MicroCaseTestForm>> getTests(@PathVariable String caseId, HttpServletRequest request) {
        return ResponseEntity.ok(resultService.getTests(caseId, authenticatedUserId(request)));
    }

    @PostMapping("/{analysisId}/results")
    public ResponseEntity<List<MicroCaseTestForm>> saveResults(@PathVariable String caseId,
            @PathVariable String analysisId, @RequestBody MicroCaseResultRequestForm body, HttpServletRequest request) {
        return ResponseEntity
                .ok(resultService.saveResults(caseId, analysisId, body, authenticatedUserId(request), request));
    }

    @PostMapping("/{analysisId}/tested-elsewhere")
    public ResponseEntity<List<MicroCaseTestForm>> setTestedElsewhere(@PathVariable String caseId,
            @PathVariable String analysisId, @RequestBody MicroCaseTestedElsewhereRequestForm body,
            HttpServletRequest request) {
        return ResponseEntity
                .ok(resultService.setTestedElsewhere(caseId, analysisId, body, authenticatedUserId(request)));
    }
}
