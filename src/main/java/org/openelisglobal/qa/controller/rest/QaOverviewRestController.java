package org.openelisglobal.qa.controller.rest;

import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.qa.dto.QaOverviewSummary;
import org.openelisglobal.qa.service.QaOverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint backing the QA Overview page (OGC-694 WS-F). Gated on the
 * qa.view.overview permission key (FRS §6 registry, liquibase/qa/004) — the
 * pre-registry Reception/Results/Validation audience keeps access via compat
 * grants; the GLOBAL_ADMIN fallback covers isAdmin-flag users without the
 * Global Administrator role row.
 */
@RestController
@RequestMapping("/rest/qa/overview")
@PreAuthorize("hasAuthority('qa.view.overview') or hasRole('GLOBAL_ADMIN')")
public class QaOverviewRestController extends BaseRestController {

    @Autowired
    private QaOverviewService qaOverviewService;

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QaOverviewSummary> getSummary() {
        return ResponseEntity.ok(qaOverviewService.getSummary());
    }
}
