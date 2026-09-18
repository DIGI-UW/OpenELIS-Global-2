package org.openelisglobal.reports.amendment.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reports.amendment.bean.AmendmentBreakdownResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentDetailResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentTrendResponse;
import org.openelisglobal.reports.amendment.service.AmendmentReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rest/reports/amendment")
// qa.view.qi is the QA-registry visibility key (liquibase/qa/004); the
// pre-registry RESULTS/REPORTS role gate stays authoritative alongside it.
@PreAuthorize("hasAuthority('qa.view.qi') or hasAnyRole('ADMIN', 'RESULTS', 'REPORTS')")
public class AmendmentReportRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AmendmentReportRestController.class);
    private static final int MAX_PAGE_SIZE = 200;
    private static final long MAX_DATE_RANGE_DAYS = 366;

    @Autowired
    private AmendmentReportService amendmentReportService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        requireAuthenticatedUser(request);

        Range range = parseRange(fromDate, toDate);

        AmendmentSummaryResponse response = amendmentReportService.getSummary(range.from(), range.to());

        logger.info("Amendment summary by user {} | range {}-{} | {} amended of {} released", getSysUserId(request),
                fromDate, toDate, response.getAmendedCount(), response.getReleasedCount());

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

        Range range = parseRange(fromDate, toDate);

        AmendmentDetailResponse response = amendmentReportService.getDetail(range.from(), range.to(), page, pageSize);

        logger.info("Amendment detail by user {} | range {}-{} | page {} size {} | {} total", getSysUserId(request),
                fromDate, toDate, page, pageSize, response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trend")
    public ResponseEntity<?> getTrend(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(defaultValue = "DAILY") String interval, HttpServletRequest request) {

        requireAuthenticatedUser(request);

        Range range = parseRange(fromDate, toDate);

        AmendmentTrendResponse response = amendmentReportService.getTrend(range.from(), range.to(), interval);

        logger.info("Amendment trend by user {} | range {}-{} | interval {} | {} points", getSysUserId(request),
                fromDate, toDate, interval, response.getPoints().size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/breakdown")
    public ResponseEntity<?> getBreakdown(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        requireAuthenticatedUser(request);

        Range range = parseRange(fromDate, toDate);

        AmendmentBreakdownResponse response = amendmentReportService.getBreakdown(range.from(), range.to());

        logger.info("Amendment breakdown by user {} | range {}-{} | {} tests", getSysUserId(request), fromDate, toDate,
                response.getRows().size());

        return ResponseEntity.ok(response);
    }

    private record Range(LocalDate from, LocalDate to) {
    }

    /**
     * Shared range guard: ISO dates, from before or equal to to, at most a year.
     * Bad input surfaces as 400 through {@link #handleBadInput}.
     */
    private static Range parseRange(String fromDate, String toDate) {
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            throw new BadRange("Invalid parameter: " + e.getMessage());
        }
        if (from.isAfter(to)) {
            throw new BadRange("fromDate must not be after toDate");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DATE_RANGE_DAYS) {
            throw new BadRange("Date range must not exceed 1 year");
        }
        return new Range(from, to);
    }

    /**
     * Only the range guard above raises this, so an unrelated
     * IllegalArgumentException from the service layer still surfaces as a 500
     * rather than being reported to the caller as bad input.
     */
    private static class BadRange extends IllegalArgumentException {
        BadRange(String message) {
            super(message);
        }
    }

    @ExceptionHandler(BadRange.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadInput(BadRange e) {
        return Map.of("error", e.getMessage());
    }

    /** Verify user is authenticated, throw 401 if not */
    private void requireAuthenticatedUser(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
    }
}
