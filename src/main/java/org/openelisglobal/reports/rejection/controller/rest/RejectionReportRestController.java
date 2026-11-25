package org.openelisglobal.reports.rejection.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.qa.security.QaPermissions;
import org.openelisglobal.reports.qi.QiReportRange;
import org.openelisglobal.reports.rejection.bean.RejectionBreakdownResponse;
import org.openelisglobal.reports.rejection.bean.RejectionDetailResponse;
import org.openelisglobal.reports.rejection.bean.RejectionHeatmapResponse;
import org.openelisglobal.reports.rejection.bean.RejectionSummaryResponse;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;
import org.openelisglobal.reports.rejection.service.RejectionReportService;
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
@RequestMapping("/rest/reports/rejection")
// Same gate as the amendment report: qa.view.qi is the QA-registry visibility
// key (liquibase/qa/004); the pre-registry RESULTS/REPORTS role gate stays
// authoritative alongside it.
@PreAuthorize(QaPermissions.VIEW_QI_OR_BENCH)
public class RejectionReportRestController extends BaseRestController {

    private static final Logger logger = LoggerFactory.getLogger(RejectionReportRestController.class);

    @Autowired
    private RejectionReportService rejectionReportService;

    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);

        RejectionSummaryResponse response = rejectionReportService.getSummary(range.from(), range.to());

        logger.info("Rejection summary by user {} | range {}-{} | {} rejected of {} started", getSysUserId(request),
                fromDate, toDate, response.getRejectedCount(), response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> getDetail(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int pageSize,
            HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);
        page = QiReportRange.clampPage(page);
        pageSize = QiReportRange.clampPageSize(pageSize);

        RejectionDetailResponse response = rejectionReportService.getDetail(range.from(), range.to(), page, pageSize);

        logger.info("Rejection detail by user {} | range {}-{} | page {} size {} | {} total", getSysUserId(request),
                fromDate, toDate, page, pageSize, response.getTotalCount());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trend")
    public ResponseEntity<?> getTrend(@RequestParam String fromDate, @RequestParam String toDate,
            @RequestParam(defaultValue = "DAILY") String interval, HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);

        RejectionTrendResponse response = rejectionReportService.getTrend(range.from(), range.to(), interval);

        logger.info("Rejection trend by user {} | range {}-{} | interval {} | {} points", getSysUserId(request),
                fromDate, toDate, interval, response.getPoints().size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/breakdown")
    public ResponseEntity<?> getBreakdown(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);

        RejectionBreakdownResponse response = rejectionReportService.getBreakdown(range.from(), range.to());

        logger.info("Rejection breakdown by user {} | range {}-{} | {} reasons, {} tests", getSysUserId(request),
                fromDate, toDate, response.getReasons().size(), response.getTests().size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/heatmap")
    public ResponseEntity<?> getHeatmap(@RequestParam String fromDate, @RequestParam String toDate,
            HttpServletRequest request) {

        QiReportRange range = QiReportRange.parse(fromDate, toDate);

        RejectionHeatmapResponse response = rejectionReportService.getHeatmap(range.from(), range.to());

        logger.info("Rejection heatmap by user {} | range {}-{} | {} cells", getSysUserId(request), fromDate, toDate,
                response.getCells().size());

        return ResponseEntity.ok(response);
    }
}
