package org.openelisglobal.coldstorage.service;

import java.time.LocalDate;
import java.util.List;
import org.openelisglobal.coldstorage.service.dto.FreezerDailyLogData;
import org.openelisglobal.coldstorage.service.dto.FreezerMonthlyLogData;
import org.openelisglobal.coldstorage.service.dto.FreezerWeeklyLogData;

public interface FreezerReportService {

    /**
     * Generate daily log report data for freezer temperature monitoring
     *
     * @param freezerId Optional freezer ID (null for all freezers)
     * @param startDate Start date for report
     * @param endDate   End date for report
     * @return List of daily log data
     */
    List<FreezerDailyLogData> generateDailyLogData(Long freezerId, LocalDate startDate, LocalDate endDate);

    /**
     * Generate weekly aggregated log report data
     *
     * @param freezerId Optional freezer ID (null for all freezers)
     * @param startDate Start date for report
     * @param endDate   End date for report
     * @return List of weekly log data
     */
    List<FreezerWeeklyLogData> generateWeeklyLogData(Long freezerId, LocalDate startDate, LocalDate endDate);

    /**
     * Generate monthly aggregated log report data
     *
     * @param freezerId Optional freezer ID (null for all freezers)
     * @param startDate Start date for report
     * @param endDate   End date for report
     * @return List of monthly log data
     */
    List<FreezerMonthlyLogData> generateMonthlyLogData(Long freezerId, LocalDate startDate, LocalDate endDate);

    /**
     * Generate PDF report bytes
     *
     * @param reportType Report type (daily, dailylog, weekly, weeklylog, monthly,
     *                   monthlylog)
     * @param freezerId  Optional freezer ID
     * @param startDate  Start date
     * @param endDate    End date
     * @return PDF bytes
     */
    byte[] generatePdfReport(String reportType, Long freezerId, LocalDate startDate, LocalDate endDate);
}
