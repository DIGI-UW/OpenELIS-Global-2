package org.openelisglobal.dashboard.management.service;

import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.dashboard.management.ScopeRange;
import org.openelisglobal.dashboard.management.dao.ManagementDashboardDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManagementDashboardServiceImpl implements ManagementDashboardService {

    @Autowired
    private ManagementDashboardDAO managementDashboardDAO;

    @Override
    @Transactional(readOnly = true)
    public ManagementSummary getSummary(ScopeRange scope) {
        return new ManagementSummary(ConfigurationProperties.getInstance().getPropertyValue(Property.SiteName),
                managementDashboardDAO.countSamplesReceived(scope.from(), scope.to()),
                managementDashboardDAO.getFeatures());
    }
}
