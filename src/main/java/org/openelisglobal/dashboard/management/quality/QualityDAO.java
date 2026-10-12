package org.openelisglobal.dashboard.management.quality;

import java.time.LocalDate;

public interface QualityDAO {

    /**
     * Non-conforming events whose report date falls between the two dates
     * inclusive.
     */
    long countNcesReported(LocalDate from, LocalDate to);
}
