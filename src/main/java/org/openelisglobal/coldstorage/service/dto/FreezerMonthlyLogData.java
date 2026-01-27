package org.openelisglobal.coldstorage.service.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FreezerMonthlyLogData {
    private String monthYear; // e.g., "December 2025"
    private Integer year; // For sorting
    private Integer month; // 1-12 for sorting
    private Integer readingCount; // Number of readings in this month
    private BigDecimal avgTemperature;
    private BigDecimal minTemperature;
    private BigDecimal maxTemperature;
    private BigDecimal avgHumidity;
    private Integer normalCount;
    private Integer warningCount;
    private Integer criticalCount;
    private Integer alertCount;
    private Integer daysMonitored; // Number of days with readings

    // Fields for template compatibility (not used in monthly report, but template
    // declares them)
    private String recordedAt;
    private String date;
    private String weekPeriod;
    private Integer weekNumber;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private String status;
    private Boolean alertTriggered;
}
