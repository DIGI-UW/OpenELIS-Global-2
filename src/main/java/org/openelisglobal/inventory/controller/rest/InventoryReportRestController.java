package org.openelisglobal.inventory.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.department.service.DepartmentIsolationService;
import org.openelisglobal.inventory.service.InventoryReportService;
import org.openelisglobal.inventory.service.InventoryReportService.GeneratedReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/inventory/reports")
public class InventoryReportRestController {

    @Autowired
    private InventoryReportService inventoryReportService;

    @Autowired
    private DepartmentIsolationService departmentIsolationService;

    @PostMapping("/generate")
    public void generateReport(@RequestParam String reportType, @RequestParam(defaultValue = "PDF") String exportFormat,
            @RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestParam(defaultValue = "true") boolean includeExpired,
            @RequestParam(defaultValue = "false") boolean groupByType, @RequestParam(defaultValue = "false") boolean groupByLocation,
            HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            if (!departmentIsolationService.hasUnrestrictedDepartmentAccess(request)) {
                response.sendError(HttpStatus.FORBIDDEN.value(), "Access denied");
                return;
            }
            GeneratedReport report = inventoryReportService.generateReport(reportType, exportFormat, startDate, endDate,
                    includeInactive, includeExpired, groupByType, groupByLocation);

            response.setStatus(HttpStatus.OK.value());
            response.setContentType(report.getContentType());
            response.setHeader("Content-Disposition", "attachment; filename=\"" + report.getFileName() + "\"");
            response.setContentLength(report.getContent().length);
            response.getOutputStream().write(report.getContent());
            response.getOutputStream().flush();
        } catch (IllegalArgumentException e) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "generateReport", e.getMessage());
            response.sendError(HttpStatus.BAD_REQUEST.value(), e.getMessage());
        } catch (Exception e) {
            LogEvent.logError(e);
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Failed to generate inventory report");
        }
    }
}
