package org.openelisglobal.reports.tat.controller.rest;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATDetailResponse;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.bean.TATTrendResponse;
import org.openelisglobal.reports.tat.service.TATReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/reports/tat")
public class TATReportRestController extends BaseRestController {

    @Autowired
    private TATReportService tatReportService;

    @GetMapping("/summary")
    public ResponseEntity<TATSummaryResponse> getSummary(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam String segment, @RequestParam(defaultValue = "CALENDAR") String calculationMode,
            @RequestParam(required = false) String labUnitIds, @RequestParam(required = false) String testIds,
            @RequestParam(required = false) String panelIds, @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer sampleTypeId,
            @RequestParam(required = false) Integer orderingSiteId,
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @RequestParam(defaultValue = "LAB_UNIT") String breakdownBy) {

        TATSummaryResponse response = tatReportService.getSummary(LocalDate.parse(fromDate), LocalDate.parse(toDate),
                TATSegment.valueOf(segment), TATCalculationMode.valueOf(calculationMode), labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled, breakdownBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/detail")
    public ResponseEntity<TATDetailResponse> getDetail(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam String segment, @RequestParam(defaultValue = "CALENDAR") String calculationMode,
            @RequestParam(required = false) String labUnitIds, @RequestParam(required = false) String testIds,
            @RequestParam(required = false) String panelIds, @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer sampleTypeId,
            @RequestParam(required = false) Integer orderingSiteId,
            @RequestParam(defaultValue = "false") boolean includeCancelled, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int pageSize,
            @RequestParam(defaultValue = "selectedTat") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String breakdownFilter) {

        TATDetailResponse response = tatReportService.getDetail(LocalDate.parse(fromDate), LocalDate.parse(toDate),
                TATSegment.valueOf(segment), TATCalculationMode.valueOf(calculationMode), labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled, page, pageSize, sortField, sortOrder,
                breakdownFilter);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trend")
    public ResponseEntity<TATTrendResponse> getTrend(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam String segment, @RequestParam(defaultValue = "CALENDAR") String calculationMode,
            @RequestParam(required = false) String labUnitIds, @RequestParam(required = false) String testIds,
            @RequestParam(required = false) String panelIds, @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer sampleTypeId,
            @RequestParam(required = false) Integer orderingSiteId,
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @RequestParam(defaultValue = "DAILY") String interval, @RequestParam(required = false) String compareBy) {

        TATTrendResponse response = tatReportService.getTrend(LocalDate.parse(fromDate), LocalDate.parse(toDate),
                TATSegment.valueOf(segment), TATCalculationMode.valueOf(calculationMode), labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled, interval, compareBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    public void export(@RequestParam String fromDate, @RequestParam String toDate, @RequestParam String segment,
            @RequestParam(defaultValue = "CALENDAR") String calculationMode,
            @RequestParam(required = false) String labUnitIds, @RequestParam(required = false) String testIds,
            @RequestParam(required = false) String panelIds, @RequestParam(required = false) String priority,
            @RequestParam(required = false) Integer sampleTypeId,
            @RequestParam(required = false) Integer orderingSiteId,
            @RequestParam(defaultValue = "false") boolean includeCancelled, @RequestParam String format,
            HttpServletResponse response) throws IOException {

        if ("CSV".equalsIgnoreCase(format)) {
            exportCsv(fromDate, toDate, segment, calculationMode, labUnitIds, testIds, panelIds, priority, sampleTypeId,
                    orderingSiteId, includeCancelled, response);
        } else {
            // PDF export - placeholder, will use JasperReports or iText in M5
            response.setStatus(501);
            response.getWriter().write("PDF export not yet implemented");
        }
    }

    private void exportCsv(String fromDate, String toDate, String segment, String calculationMode, String labUnitIds,
            String testIds, String panelIds, String priority, Integer sampleTypeId, Integer orderingSiteId,
            boolean includeCancelled, HttpServletResponse httpResponse) throws IOException {

        List<TATResult> results = tatReportService.getAllResults(LocalDate.parse(fromDate), LocalDate.parse(toDate),
                TATSegment.valueOf(segment), TATCalculationMode.valueOf(calculationMode), labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled);

        httpResponse.setContentType("text/csv");
        httpResponse.setHeader("Content-Disposition",
                "attachment; filename=\"tat-report-" + fromDate + "-to-" + toDate + ".csv\"");

        PrintWriter writer = httpResponse.getWriter();
        // Header
        writer.println("Lab Number,Test,Lab Unit,Priority,Sample Type,Ordering Site,"
                + "Order Created,Collected,Received,Testing Started,Result Entered,Validated,"
                + "Selected Segment TAT (hours),Overall TAT (hours)");

        DateTimeFormatter isoFmt = DateTimeFormatter.ISO_DATE_TIME;
        for (TATResult r : results) {
            writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n", csv(r.getLabNumber()), csv(r.getTestName()),
                    csv(r.getLabUnit()), csv(r.getPriority()), csv(r.getSampleType()), csv(r.getOrderingSite()),
                    r.getOrderCreated() != null ? r.getOrderCreated().toInstant().toString() : "",
                    r.getCollected() != null ? r.getCollected().toInstant().toString() : "",
                    r.getReceived() != null ? r.getReceived().toInstant().toString() : "",
                    r.getTestingStarted() != null ? r.getTestingStarted().toInstant().toString() : "",
                    r.getResultEntered() != null ? r.getResultEntered().toInstant().toString() : "",
                    r.getValidated() != null ? r.getValidated().toInstant().toString() : "",
                    r.getSelectedSegmentTat() != null ? r.getSelectedSegmentTat().toPlainString() : "",
                    r.getOverallTat() != null ? r.getOverallTat().toPlainString() : "");
        }
        writer.flush();
    }

    private String csv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
