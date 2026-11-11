package org.openelisglobal.dashboard.management.service;

import org.openelisglobal.dashboard.management.ScopeRange;

public interface ManagementDashboardService {

    ManagementSummary getSummary(ScopeRange scope);
}
