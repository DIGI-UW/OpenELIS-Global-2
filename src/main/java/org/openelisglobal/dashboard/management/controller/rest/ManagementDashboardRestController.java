package org.openelisglobal.dashboard.management.controller.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dashboard.management.DashboardPermissions;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.antibiogram.AntibiogramResponse;
import org.openelisglobal.dashboard.management.antibiogram.AntibiogramService;
import org.openelisglobal.dashboard.management.equipment.AnalyzerActivity;
import org.openelisglobal.dashboard.management.equipment.EquipmentService;
import org.openelisglobal.dashboard.management.kpi.KpiService;
import org.openelisglobal.dashboard.management.kpi.ManagementKpis;
import org.openelisglobal.dashboard.management.quality.QualityRollup;
import org.openelisglobal.dashboard.management.quality.QualityRollupService;
import org.openelisglobal.dashboard.management.service.ManagementDashboardService;
import org.openelisglobal.dashboard.management.service.ManagementSummary;
import org.openelisglobal.dashboard.management.stock.StockResponse;
import org.openelisglobal.dashboard.management.stock.StockService;
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

    @Autowired
    private AntibiogramService antibiogramService;

    @Autowired
    private StockService stockService;

    @Autowired
    private QualityRollupService qualityRollupService;

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

    @GetMapping(value = "/antibiogram", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AntibiogramResponse> getAntibiogram(@RequestParam(defaultValue = "30d") String scope) {
        return respond(scope, antibiogramService::getAntibiogram);
    }

    @GetMapping(value = "/stock", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StockResponse> getStock() {
        return ResponseEntity.ok(stockService.getStock());
    }

    @GetMapping(value = "/quality", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QualityRollup> getQuality(@RequestParam(defaultValue = "30d") String scope) {
        return respond(scope, qualityRollupService::getRollup);
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
