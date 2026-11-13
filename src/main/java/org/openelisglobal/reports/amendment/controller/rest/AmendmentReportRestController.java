package org.openelisglobal.reports.amendment.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.qa.security.QaPermissions;
import org.openelisglobal.reports.amendment.bean.AmendmentBreakdownResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentDetailResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentTrendResponse;
import org.openelisglobal.reports.amendment.service.AmendmentReportService;
import org.openelisglobal.reports.qi.QiReportRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/reports/amendment")
// qa.view.qi is the QA-registry visibility key (liquibase/qa/004); the
// pre-registry RESULTS/REPORTS role gate stays authoritative alongside it.
@PreAuthorize(QaPermissions.VIEW_QI_OR_BENCH)
public class AmendmentReportRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(AmendmentReportRestController.class);

    @Autowired
    private AmendmentReportService amendmentReportService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);

        AmendmentSummaryResponse response = amendmentReportService.getSummary(range.from(), range.to());

        logger.info("Amendment summary by user {} | range {}-{} | {} amended of {} released", getSysUserId(request),
                fromDate, toDate, response.getAmendedCount(), response.getReleasedCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int pageSize,
            HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);
        page = QiReportRange.clampPage(page);
        pageSize = QiReportRange.clampPageSize(pageSize);

        AmendmentDetailResponse response = amendmentReportService.getDetail(range.from(), range.to(), page, pageSize);

        logger.info("Amendment detail by user {} | range {}-{} | page {} size {} | {} total", getSysUserId(request),
                fromDate, toDate, page, pageSize, response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trend")
    public ResponseEntity<?> getTrend(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(defaultValue = "DAILY") String interval, HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);

        AmendmentTrendResponse response = amendmentReportService.getTrend(range.from(), range.to(), interval);

        logger.info("Amendment trend by user {} | range {}-{} | interval {} | {} points", getSysUserId(request),
                fromDate, toDate, interval, response.getPoints().size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/breakdown")
    public ResponseEntity<?> getBreakdown(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);

        AmendmentBreakdownResponse response = amendmentReportService.getBreakdown(range.from(), range.to());

        logger.info("Amendment breakdown by user {} | range {}-{} | {} tests", getSysUserId(request), fromDate, toDate,
                response.getRows().size());

        return ResponseEntity.ok(response);
    }
}
