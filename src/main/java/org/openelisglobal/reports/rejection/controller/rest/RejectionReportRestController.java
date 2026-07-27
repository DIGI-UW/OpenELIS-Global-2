package org.openelisglobal.reports.rejection.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reports.rejection.bean.RejectionBreakdownResponse;
import org.openelisglobal.reports.rejection.bean.RejectionDetailResponse;
import org.openelisglobal.reports.rejection.bean.RejectionHeatmapResponse;
import org.openelisglobal.reports.rejection.bean.RejectionSummaryResponse;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;
import org.openelisglobal.reports.rejection.service.RejectionReportService;
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
@RequestMapping("/rest/reports/rejection")
// Same gate as the amendment report: qa.view.qi is the QA-registry visibility
// key (liquibase/qa/004); the pre-registry RESULTS/REPORTS role gate stays
// authoritative alongside it.
@PreAuthorize("hasAuthority('qa.view.qi') or hasAnyRole('ADMIN', 'RESULTS', 'REPORTS')")
public class RejectionReportRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(RejectionReportRestController.class);
    private static final int MAX_PAGE_SIZE = 200;
    private static final long MAX_DATE_RANGE_DAYS = 366;

    @Autowired
    private RejectionReportService rejectionReportService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        requireAuthenticatedUser(request);

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        ResponseEntity<?> rangeError = validateRange(from, to);
        if (rangeError != null) {
            return rangeError;
        }

        RejectionSummaryResponse response = rejectionReportService.getSummary(from, to);

        logger.info("Rejection summary by user {} | range {}-{} | {} rejected of {} started", getSysUserId(request),
                fromDate, toDate, response.getRejectedCount(), response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int pageSize,
            HttpServletRequest request) {

        requireAuthenticatedUser(request);

        if (page < 0) {
            page = 0;
        }
        if (pageSize < 1) {
            pageSize = 25;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        ResponseEntity<?> rangeError = validateRange(from, to);
        if (rangeError != null) {
            return rangeError;
        }

        RejectionDetailResponse response = rejectionReportService.getDetail(from, to, page, pageSize);

        logger.info("Rejection detail by user {} | range {}-{} | page {} size {} | {} total", getSysUserId(request),
                fromDate, toDate, page, pageSize, response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trend")
    public ResponseEntity<?> getTrend(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(defaultValue = "DAILY") String interval, HttpServletRequest request) {

        requireAuthenticatedUser(request);

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        ResponseEntity<?> rangeError = validateRange(from, to);
        if (rangeError != null) {
            return rangeError;
        }

        RejectionTrendResponse response = rejectionReportService.getTrend(from, to, interval);

        logger.info("Rejection trend by user {} | range {}-{} | interval {} | {} points", getSysUserId(request),
                fromDate, toDate, interval, response.getPoints().size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/breakdown")
    public ResponseEntity<?> getBreakdown(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        requireAuthenticatedUser(request);

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        ResponseEntity<?> rangeError = validateRange(from, to);
        if (rangeError != null) {
            return rangeError;
        }

        RejectionBreakdownResponse response = rejectionReportService.getBreakdown(from, to);

        logger.info("Rejection breakdown by user {} | range {}-{} | {} reasons, {} tests", getSysUserId(request),
                fromDate, toDate, response.getReasons().size(), response.getTests().size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<?> getHeatmap(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        requireAuthenticatedUser(request);

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        ResponseEntity<?> rangeError = validateRange(from, to);
        if (rangeError != null) {
            return rangeError;
        }

        RejectionHeatmapResponse response = rejectionReportService.getHeatmap(from, to);

        logger.info("Rejection heatmap by user {} | range {}-{} | {} cells", getSysUserId(request), fromDate, toDate,
                response.getCells().size());

        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of("error", "fromDate must not be after toDate"));
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DATE_RANGE_DAYS) {
            return ResponseEntity.badRequest().body(Map.of("error", "Date range must not exceed 1 year"));
        }
        return null;
    }

    /** Verify user is authenticated, throw 401 if not */
    private void requireAuthenticatedUser(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
    }
}
