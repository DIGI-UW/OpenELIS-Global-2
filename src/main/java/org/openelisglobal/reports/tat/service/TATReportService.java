package org.openelisglobal.reports.tat.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATDetailResponse;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.bean.TATTrendResponse;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TATReportService {

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    TATSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, String sampleTypeId,
            String orderingSiteId, boolean includeCancelled, String breakdownBy);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    TATDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, String sampleTypeId,
            String orderingSiteId, boolean includeCancelled, int page, int pageSize, String sortField, String sortOrder,
            String breakdownFilter, String breakdownDimension);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    TATTrendResponse getTrend(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, String sampleTypeId,
            String orderingSiteId, boolean includeCancelled, String interval, String compareBy);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    List<TATResult> getAllResults(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, String sampleTypeId,
            String orderingSiteId, boolean includeCancelled);
}
