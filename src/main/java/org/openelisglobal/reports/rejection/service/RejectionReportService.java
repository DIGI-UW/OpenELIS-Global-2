package org.openelisglobal.reports.rejection.service;

import java.time.LocalDate;
import org.openelisglobal.reports.rejection.bean.RejectionBreakdownResponse;
import org.openelisglobal.reports.rejection.bean.RejectionDetailResponse;
import org.openelisglobal.reports.rejection.bean.RejectionHeatmapResponse;
import org.openelisglobal.reports.rejection.bean.RejectionSummaryResponse;
import org.openelisglobal.reports.rejection.bean.RejectionTrendResponse;

public interface RejectionReportService {

    RejectionSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate);

    RejectionDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, int page, int pageSize);

    RejectionTrendResponse getTrend(LocalDate fromDate, LocalDate toDate, String interval);

    RejectionBreakdownResponse getBreakdown(LocalDate fromDate, LocalDate toDate);

    RejectionHeatmapResponse getHeatmap(LocalDate fromDate, LocalDate toDate);
}
