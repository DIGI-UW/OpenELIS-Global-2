package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseInoculationForm;
import org.openelisglobal.microbiology.form.MicroCaseInoculationRequestForm;
import org.openelisglobal.microbiology.service.MicroCaseInoculationService;
import org.openelisglobal.microbiology.valueholder.MicroCaseInoculation;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/inoculations")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCaseInoculationRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseInoculationService inoculationService;

    public MicroCaseInoculationRestController(MicroCaseInoculationService inoculationService) {
        this.inoculationService = inoculationService;
    }

    @GetMapping
    public ResponseEntity<List<MicroCaseInoculationForm>> getByCaseId(@PathVariable String caseId) {
        return ResponseEntity.ok(inoculationService.getByCaseId(caseId));
    }

    @PostMapping
    public ResponseEntity<MicroCaseInoculationForm> record(@PathVariable String caseId,
            @RequestBody MicroCaseInoculationRequestForm request, HttpServletRequest httpRequest) {
        MicroCaseInoculation inoculation = inoculationService.record(caseId, request.sourceInoculationId,
                request.containerIdentifier, request.media, request.incubation, request.atmosphere,
                lotSelections(request.lotSelections), authenticatedUserId(httpRequest));
        return ResponseEntity.ok(inoculationService.getByCaseId(caseId).stream()
                .filter(form -> inoculation.getId().equals(form.id)).findFirst().orElseThrow());
    }
}
