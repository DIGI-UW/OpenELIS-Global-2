package org.openelisglobal.dashboard.management.controller.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dashboard.management.DashboardPermissions;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.equipment.AnalyzerActivity;
import org.openelisglobal.dashboard.management.equipment.EquipmentService;
import org.openelisglobal.dashboard.management.kpi.KpiService;
import org.openelisglobal.dashboard.management.kpi.ManagementKpis;
import org.openelisglobal.dashboard.management.service.ManagementDashboardService;
import org.openelisglobal.dashboard.management.service.ManagementSummary;
import org.openelisglobal.dashboard.management.workload.WorkloadResponse;
import org.openelisglobal.dashboard.management.workload.WorkloadService;
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

    @Autowired
    private KpiService kpiService;

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private EquipmentService equipmentService;

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManagementSummary> getSummary(@RequestParam(defaultValue = "30d") String scope) {
        return respond(scope, managementDashboardService::getSummary);
    }

    @GetMapping(value = "/kpis", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ManagementKpis> getKpis(@RequestParam(defaultValue = "30d") String scope) {
        return respond(scope, kpiService::getKpis);
    }

    @GetMapping(value = "/workload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WorkloadResponse> getWorkload(@RequestParam(defaultValue = "30d") String scope) {
        return respond(scope, workloadService::getWorkload);
    }

    @GetMapping(value = "/equipment", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<AnalyzerActivity>> getEquipment(@RequestParam(defaultValue = "30d") String scope) {
        return respond(scope, equipmentService::getEquipment);
    }

    private static <T> ResponseEntity<T> respond(String scope, Function<ScopeRange, T> read) {
        ScopeRange range;
        try {
            range = ScopeRange.of(scope, LocalDate.now());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(read.apply(range));
    }
}
