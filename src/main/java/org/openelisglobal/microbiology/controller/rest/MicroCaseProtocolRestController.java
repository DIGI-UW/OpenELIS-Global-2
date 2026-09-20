package org.openelisglobal.microbiology.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.openelisglobal.microbiology.form.MicroCaseDetailForm;
import org.openelisglobal.microbiology.form.MicroCaseProtocolChangeRequestForm;
import org.openelisglobal.microbiology.form.MicroCaseProtocolOptionForm;
import org.openelisglobal.microbiology.service.MicroCaseProtocolService;
import org.openelisglobal.microbiology.service.MicroCaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/cases/{caseId}/protocol")
@PreAuthorize(MicrobiologyRestControllerSupport.BENCH_ACCESS)
public class MicroCaseProtocolRestController extends MicrobiologyRestControllerSupport {

    private final MicroCaseProtocolService protocolService;
    private final MicroCaseService caseService;

    public MicroCaseProtocolRestController(MicroCaseProtocolService protocolService, MicroCaseService caseService) {
        this.protocolService = protocolService;
        this.caseService = caseService;
    }

    @GetMapping("/options")
    public ResponseEntity<List<MicroCaseProtocolOptionForm>> getOptions(@PathVariable String caseId) {
        return ResponseEntity.ok(protocolService.getProtocolOptions(caseId));
    }

    @PutMapping
    public ResponseEntity<MicroCaseDetailForm> changeProtocol(@PathVariable String caseId,
            @Valid @RequestBody MicroCaseProtocolChangeRequestForm request, HttpServletRequest httpRequest) {
        protocolService.changeProtocol(caseId, request.cultureMethodId, request.reason,
                authenticatedUserId(httpRequest));
        return ResponseEntity.ok(caseService.getCaseDetail(caseId));
    }
}
