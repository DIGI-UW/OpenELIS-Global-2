package org.openelisglobal.dashboard.management.controller.rest;

import java.time.LocalDate;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dashboard.management.DashboardPermissions;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.service.ManagementDashboardService;
import org.openelisglobal.dashboard.management.service.ManagementSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/dashboards/management")
@PreAuthorize(DashboardPermissions.VIEW_MANAGEMENT)
public class ManagementDashboardRestController extends BaseRestController {

    @Autowired
    private ManagementDashboardService managementDashboardService;

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManagementSummary> getSummary(@RequestParam(defaultValue = "30d") String scope) {
        ScopeRange range;
        try {
            range = ScopeRange.of(scope, LocalDate.now());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(managementDashboardService.getSummary(range));
    }
}
