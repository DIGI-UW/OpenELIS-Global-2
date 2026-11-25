package org.openelisglobal.dashboard.management.workload;

import java.time.LocalDate;
import java.util.List;

public interface WorkloadDAO {

    /** Rows of {day (yyyy-MM-dd), test section id, distinct samples received}. */
    List<Object[]> countSamplesByDayAndSection(LocalDate from, LocalDate to);
}
