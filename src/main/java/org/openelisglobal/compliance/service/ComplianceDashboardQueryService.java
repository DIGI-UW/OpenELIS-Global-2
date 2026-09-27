package org.openelisglobal.compliance.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.compliance.controller.rest.dto.DashboardSummaryDTO;
import org.openelisglobal.compliance.controller.rest.dto.DashboardTrendDTO;
import org.openelisglobal.compliance.controller.rest.dto.PagedExceedanceDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteComparisonDTO;
import org.openelisglobal.compliance.controller.rest.dto.SiteParameterTrendDTO;

public interface ComplianceDashboardQueryService {

    DashboardSummaryDTO getSummary(List<String> siteIds, String standardId, LocalDate start, LocalDate end);

    DashboardTrendDTO getTrend(List<String> siteIds, String standardId, LocalDate start, LocalDate end);

    SiteParameterTrendDTO getSiteParameters(String siteId, String standardId, LocalDate start, LocalDate end);

    List<SiteComparisonDTO> getSiteComparison(String standardId, LocalDate start, LocalDate end);

    PagedExceedanceDTO getExceedances(List<String> siteIds, String standardId, LocalDate start, LocalDate end, int page,
            int size, String sortBy, String sortDir);
}
