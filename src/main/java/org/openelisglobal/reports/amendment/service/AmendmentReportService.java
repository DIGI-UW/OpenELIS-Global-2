package org.openelisglobal.reports.amendment.service;

import java.time.LocalDate;
import org.openelisglobal.reports.amendment.bean.AmendmentDetailResponse;
import org.openelisglobal.reports.amendment.bean.AmendmentSummaryResponse;

public interface AmendmentReportService {

    AmendmentSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate);

    AmendmentDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, int page, int pageSize);
}
