package org.openelisglobal.microbiology.controller.rest;

import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.microbiology.form.MicroWorklistRowForm;
import org.openelisglobal.microbiology.service.MicroWorklistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/microbiology/worklist")
public class MicroWorklistRestController extends BaseRestController {

    private final MicroWorklistService worklistService;

    public MicroWorklistRestController(MicroWorklistService worklistService) {
        this.worklistService = worklistService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MicroWorklistRowForm>> getWorklistRows() {
        return ResponseEntity.ok(worklistService.getWorklistRows());
    }
}
