package org.openelisglobal.compliance.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.compliance.controller.rest.dto.DashboardSummaryDTO;
import org.openelisglobal.compliance.controller.rest.dto.DashboardTrendDTO;
import org.openelisglobal.compliance.controller.rest.dto.PagedExceedanceDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteComparisonDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteParameterTrendDTO;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Also accepts PRIV_RESULT_VIEW: the dashboard summarises environmental RESULTS
 * (exceedance rows carry the measured value), and /EnvironmentalDashboard is
 * routed to Results on result:enter. On PRIV_REPORT_RUN alone every read 403'd
 * for Results; granting report:run instead would have opened 40+ reporting
 * methods.
 */
public interface ComplianceDashboardQueryService {

    @PreAuthorize("hasAnyAuthority('PRIV_REPORT_RUN','PRIV_RESULT_VIEW')")
    DashboardSummaryDTO getSummary(List<String> siteIds, String standardId, LocalDate start, LocalDate end);

    @PreAuthorize("hasAnyAuthority('PRIV_REPORT_RUN','PRIV_RESULT_VIEW')")
    DashboardTrendDTO getTrend(List<String> siteIds, String standardId, LocalDate start, LocalDate end);

    @PreAuthorize("hasAnyAuthority('PRIV_REPORT_RUN','PRIV_RESULT_VIEW')")
    SiteParameterTrendDTO getSiteParameters(String siteId, String standardId, LocalDate start, LocalDate end);

    @PreAuthorize("hasAnyAuthority('PRIV_REPORT_RUN','PRIV_RESULT_VIEW')")
    List<SiteComparisonDTO> getSiteComparison(String standardId, LocalDate start, LocalDate end);

    @PreAuthorize("hasAnyAuthority('PRIV_REPORT_RUN','PRIV_RESULT_VIEW')")
    PagedExceedanceDTO getExceedances(List<String> siteIds, String standardId, LocalDate start, LocalDate end, int page,
            int size, String sortBy, String sortDir);
}
