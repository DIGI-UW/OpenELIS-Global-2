package org.openelisglobal.reports.amendment.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.reports.amendment.bean.AmendmentDetailResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;
import org.openelisglobal.reports.amendment.service.AmendmentReportService;
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
@RequestMapping("/rest/reports/amendment")
@PreAuthorize("hasAnyRole('ADMIN', 'RESULTS', 'REPORTS')")
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

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of("error", "fromDate must not be after toDate"));
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DATE_RANGE_DAYS) {
            return ResponseEntity.badRequest().body(Map.of("error", "Date range must not exceed 1 year"));
        }

        AmendmentSummaryResponse response = amendmentReportService.getSummary(from, to);

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

        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(fromDate);
            to = LocalDate.parse(toDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid parameter: " + e.getMessage()));
        }

        if (from.isAfter(to)) {
            return ResponseEntity.badRequest().body(Map.of("error", "fromDate must not be after toDate"));
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DATE_RANGE_DAYS) {
            return ResponseEntity.badRequest().body(Map.of("error", "Date range must not exceed 1 year"));
        }

        AmendmentDetailResponse response = amendmentReportService.getDetail(from, to, page, pageSize);

        logger.info("Amendment detail by user {} | range {}-{} | page {} size {} | {} total", getSysUserId(request),
                fromDate, toDate, page, pageSize, response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    /** Verify user is authenticated, throw 401 if not */
    private void requireAuthenticatedUser(HttpServletRequest request) {
        String userId = getSysUserId(request);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
    }
}
