package org.openelisglobal.coldstorage.service.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FreezerDailyLogData {
    private String recordedAt; // Full timestamp: "2025-12-03 14:30:00"
    private String date; // Date only: "2025-12-03" - for grouping
    private String monthYear; // e.g., "December 2025" - for month grouping
    private String weekPeriod; // e.g., "Week 1: Dec 02-08, 2025" - for week grouping
    private Integer year; // For sorting
    private Integer month; // 1-12 for sorting
    private Integer weekNumber; // 1-5 for sorting within month
    private BigDecimal temperature;
    private BigDecimal humidity;
    private String status; // NORMAL, WARNING, CRITICAL
    private Boolean alertTriggered;

    // Fields for template compatibility (not used in daily report, but template
    // declares them)
    private Integer readingCount;
    private BigDecimal avgTemperature;
    private BigDecimal minTemperature;
    private BigDecimal maxTemperature;
    private BigDecimal avgHumidity;
    private Integer normalCount;
    private Integer warningCount;
    private Integer criticalCount;
    private Integer alertCount;
    private Integer daysMonitored;
}
