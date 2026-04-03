package org.openelisglobal.reports.tat.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.reports.tat.bean.TATCalculationMode;
import org.openelisglobal.reports.tat.bean.TATDetailResponse;
import org.openelisglobal.reports.tat.bean.TATResult;
import org.openelisglobal.reports.tat.bean.TATSegment;
import org.openelisglobal.reports.tat.bean.TATSummaryResponse;
import org.openelisglobal.reports.tat.bean.TATTrendResponse;

public interface TATReportService {

    TATSummaryResponse getSummary(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, Integer sampleTypeId,
            Integer orderingSiteId, boolean includeCancelled, String breakdownBy);

    TATDetailResponse getDetail(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, Integer sampleTypeId,
            Integer orderingSiteId, boolean includeCancelled, int page, int pageSize, String sortField,
            String sortOrder, String breakdownFilter);

    TATTrendResponse getTrend(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, Integer sampleTypeId,
            Integer orderingSiteId, boolean includeCancelled, String interval, String compareBy);

    List<TATResult> getAllResults(LocalDate fromDate, LocalDate toDate, TATSegment segment, TATCalculationMode mode,
            String labUnitIds, String testIds, String panelIds, String priority, Integer sampleTypeId,
            Integer orderingSiteId, boolean includeCancelled);
}
