package org.openelisglobal.compliance.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.compliance.controller.rest.dto.DashboardSummaryDTO;
import org.openelisglobal.compliance.controller.rest.dto.DashboardTrendDTO;
import org.openelisglobal.compliance.controller.rest.dto.PagedExceedanceDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteComparisonDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteParameterTrendDTO;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ComplianceDashboardQueryService {

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    DashboardSummaryDTO getSummary(List<String> siteIds, String standardId, LocalDate start, LocalDate end);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    DashboardTrendDTO getTrend(List<String> siteIds, String standardId, LocalDate start, LocalDate end);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    SiteParameterTrendDTO getSiteParameters(String siteId, String standardId, LocalDate start, LocalDate end);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    List<SiteComparisonDTO> getSiteComparison(String standardId, LocalDate start, LocalDate end);

    @PreAuthorize("hasAuthority('PRIV_REPORT_RUN')")
    PagedExceedanceDTO getExceedances(List<String> siteIds, String standardId, LocalDate start, LocalDate end, int page,
            int size, String sortBy, String sortDir);
}
