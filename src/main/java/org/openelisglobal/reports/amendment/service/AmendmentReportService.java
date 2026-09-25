package org.openelisglobal.reports.amendment.service;

import java.time.LocalDate;
import org.openelisglobal.reports.amendment.bean.AmendmentBreakdownResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentDetailResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentTrendResponse;

public interface AmendmentReportService {

    AmendmentSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate);

    AmendmentDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, int page, int pageSize);

    AmendmentTrendResponse getTrend(LocalDate fromDate, LocalDate toDate, String interval);

    AmendmentBreakdownResponse getBreakdown(LocalDate fromDate, LocalDate toDate);
}
