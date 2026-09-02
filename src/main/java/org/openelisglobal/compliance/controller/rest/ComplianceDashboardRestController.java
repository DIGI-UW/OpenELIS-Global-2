package org.openelisglobal.compliance.controller.rest;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.openelisglobal.compliance.controller.rest.dto.DashboardSummaryDTO;
import org.openelisglobal.compliance.controller.rest.dto.DashboardTrendDTO;
import org.openelisglobal.compliance.controller.rest.dto.PagedExceedanceDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteComparisonDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteParameterTrendDTO;
import org.openelisglobal.compliance.service.ComplianceDashboardQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/compliance/dashboard")
@PreAuthorize("hasAnyRole('GLOBAL_ADMIN', 'RECEPTION', 'RESULTS', 'VALIDATION')")
public class ComplianceDashboardRestController {

    @Autowired
    private ComplianceDashboardQueryService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(@RequestParam(required = false) String siteIds,
            @RequestParam(required = false) String standardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(12);
        return ResponseEntity.ok(dashboardService.getSummary(parseSiteIds(siteIds), standardId, start, end));
    }

    @GetMapping("/trend")
    public ResponseEntity<DashboardTrendDTO> getTrend(@RequestParam(required = false) String siteIds,
            @RequestParam(required = false) String standardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(12);
        return ResponseEntity.ok(dashboardService.getTrend(parseSiteIds(siteIds), standardId, start, end));
    }

    @GetMapping("/sites/{siteId}/parameters")
    public ResponseEntity<SiteParameterTrendDTO> getSiteParameters(@PathVariable String siteId,
            @RequestParam(required = false) String standardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(12);
        return ResponseEntity.ok(dashboardService.getSiteParameters(siteId, standardId, start, end));
    }

    @GetMapping("/sites/comparison")
    public ResponseEntity<List<SiteComparisonDTO>> getSiteComparison(@RequestParam(required = false) String standardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(12);
        return ResponseEntity.ok(dashboardService.getSiteComparison(standardId, start, end));
    }

    @GetMapping("/exceedances")
    public ResponseEntity<PagedExceedanceDTO> getExceedances(@RequestParam(required = false) String siteIds,
            @RequestParam(required = false) String standardId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "date") String sortBy, @RequestParam(defaultValue = "desc") String sortDir) {
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        LocalDate start = startDate != null ? startDate : end.minusMonths(12);
        return ResponseEntity.ok(dashboardService.getExceedances(parseSiteIds(siteIds), standardId, start, end, page,
                size, sortBy, sortDir));
    }

    private List<String> parseSiteIds(String siteIds) {
        if (siteIds == null || siteIds.isBlank())
            return null;
        return Arrays.asList(siteIds.split(","));
    }
}
