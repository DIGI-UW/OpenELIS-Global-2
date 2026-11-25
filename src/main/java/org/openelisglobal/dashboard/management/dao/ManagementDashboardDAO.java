package org.openelisglobal.dashboard.management.dao;

import java.time.LocalDate;
import java.util.Map;

public interface ManagementDashboardDAO {

    long countSamplesReceived(LocalDate from, LocalDate to);

    /** NCEs by report date, both ends inclusive. */
    long countNcesReported(LocalDate from, LocalDate to);

    Map<String, Boolean> getFeatures();
}
