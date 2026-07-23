package org.openelisglobal.reports.tat.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATDetailResponse;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.bean.TATTrendResponse;
import org.openelisglobal.reports.tat.service.TATReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rest/reports/tat")
@PreAuthorize("hasAnyRole('ADMIN', 'RESULTS', 'REPORTS')")
public class TATReportRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(TATReportRestController.class);
    private static final int MAX_PAGE_SIZE = 200;
    private static final long MAX_DATE_RANGE_DAYS = 366;

    @Autowired
    private TATReportService tatReportService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam String segment, @RequestParam(defaultValue = "CALENDAR") String calculationMode,

            @RequestParam(required = false) String labUnitIds, @RequestParam(required = false) String testIds,
            @RequestParam(required = false) String panelIds, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String sampleTypeId, @RequestParam(required = false) String orderingSiteId,
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @RequestParam(defaultValue = "LAB_UNIT") String breakdownBy, HttpServletRequest request) {

        requireAuthenticatedUser(request);

        LocalDate from;
        LocalDate to;
        TATSegment seg;
        TATCalculationMode mode;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
            seg = TATSegment.valueOf(segment);
            mode = TATCalculationMode.valueOf(calculationMode);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of("error", "fromDate must not be after toDate"));
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DATE_RANGE_DAYS) {
            return ResponseEntity.badRequest().body(Map.of("error", "Date range must not exceed 1 year"));
        }

        TATSummaryResponse response = tatReportService.getSummary(from, to, seg, mode, labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled, breakdownBy);

        logger.info("TAT summary by user {} | range {}-{} segment {} mode {} | {} results", getSysUserId(request),
                fromDate, toDate, segment, calculationMode, response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam String segment, @RequestParam(defaultValue = "CALENDAR") String calculationMode,

            @RequestParam(required = false) String labUnitIds, @RequestParam(required = false) String testIds,
            @RequestParam(required = false) String panelIds, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String sampleTypeId, @RequestParam(required = false) String orderingSiteId,
            @RequestParam(defaultValue = "false") boolean includeCancelled, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int pageSize,
            @RequestParam(defaultValue = "selectedTat") String sortField,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String breakdownFilter,
            @RequestParam(required = false) String breakdownDimension, HttpServletRequest request) {

        requireAuthenticatedUser(request);

        if (page < 0)
            page = 0;
        if (pageSize < 1)
            pageSize = 25;
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        LocalDate from;
        LocalDate to;
        TATSegment seg;
        TATCalculationMode mode;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
            seg = TATSegment.valueOf(segment);
            mode = TATCalculationMode.valueOf(calculationMode);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of("error", "fromDate must not be after toDate"));
        }

        TATDetailResponse response = tatReportService.getDetail(from, to, seg, mode, labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled, page, pageSize, sortField, sortOrder,
                breakdownFilter, breakdownDimension);

        logger.info("TAT detail by user {} | range {}-{} segment {} | page {} size {} | {} results",
                getSysUserId(request), fromDate, toDate, segment, page, pageSize, response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trend")
    public ResponseEntity<?> getTrend(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam String segment, @RequestParam(defaultValue = "CALENDAR") String calculationMode,

            @RequestParam(required = false) String labUnitIds, @RequestParam(required = false) String testIds,
            @RequestParam(required = false) String panelIds, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String sampleTypeId, @RequestParam(required = false) String orderingSiteId,
            @RequestParam(defaultValue = "false") boolean includeCancelled,
            @RequestParam(defaultValue = "DAILY") String interval, @RequestParam(required = false) String compareBy,
            HttpServletRequest request) {

        requireAuthenticatedUser(request);

        LocalDate from;
        LocalDate to;
        TATSegment seg;
        TATCalculationMode mode;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
            seg = TATSegment.valueOf(segment);
            mode = TATCalculationMode.valueOf(calculationMode);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of("error", "fromDate must not be after toDate"));
        }

        TATTrendResponse response = tatReportService.getTrend(from, to, seg, mode, labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled, interval, compareBy);

        logger.info("TAT trend by user {} | range {}-{} segment {} interval {}", getSysUserId(request), fromDate,
                toDate, segment, interval);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    public void export(@RequestParam String fromDate, @RequestParam String toDate, @RequestParam String segment,
            @RequestParam(defaultValue = "CALENDAR") String calculationMode,

            @RequestParam(required = false) String labUnitIds, @RequestParam(required = false) String testIds,
            @RequestParam(required = false) String panelIds, @RequestParam(required = false) String priority,
            @RequestParam(required = false) String sampleTypeId, @RequestParam(required = false) String orderingSiteId,
            @RequestParam(defaultValue = "false") boolean includeCancelled, @RequestParam String format,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        requireAuthenticatedUser(request);

        if ("CSV".equalsIgnoreCase(format)) {
            exportCsv(fromDate, toDate, segment, calculationMode, labUnitIds, testIds, panelIds, priority, sampleTypeId,
                    orderingSiteId, includeCancelled, request, response);
        } else {
            response.setStatus(501);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"PDF export not yet implemented\"}");
        }
    }

    private void exportCsv(String fromDate, String toDate, String segment, String calculationMode, String labUnitIds,
            String testIds, String panelIds, String priority, String sampleTypeId, String orderingSiteId,
            boolean includeCancelled, HttpServletRequest request, HttpServletResponse httpResponse) throws IOException {

        LocalDate from;
        LocalDate to;
        TATSegment seg;
        TATCalculationMode mode;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
            seg = TATSegment.valueOf(segment);
            mode = TATCalculationMode.valueOf(calculationMode);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            httpResponse.sendError(400, "Invalid parameter: " + e.getMessage());
            return;
        }

        List<TATResult> results = tatReportService.getAllResults(from, to, seg, mode, labUnitIds, testIds, panelIds,
                priority, sampleTypeId, orderingSiteId, includeCancelled);

        // Use parsed dates for safe filename (no injection risk)
        httpResponse.setContentType("text/csv");
        httpResponse.setHeader("Content-Disposition",
                "attachment; filename=\"tat-report-" + from.toString() + "-to-" + to.toString() + ".csv\"");

        logger.info("TAT CSV export by user {} | range {}-{} segment {} | {} rows", getSysUserId(request), fromDate,
                toDate, segment, results.size());

        PrintWriter writer = httpResponse.getWriter();
        writer.println("Lab Number,Test,Lab Unit,Priority,Sample Type,Ordering Site,"
                + "Order Created,Collected,Received,Testing Started,Result Entered,Validated,"
                + "Selected Segment TAT (hours),Overall TAT (hours)");

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

    /** CSV formula injection protection + quoting */
    private String csv(String value) {
        if (value == null) {
            return "";
        }
        // Prevent CSV formula injection (=, +, -, @, \t, \r)
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Verify user is authenticated, throw 401 if not */
    private void requireAuthenticatedUser(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
    }
}
