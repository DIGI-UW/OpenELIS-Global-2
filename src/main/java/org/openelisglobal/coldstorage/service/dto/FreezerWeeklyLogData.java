package org.openelisglobal.coldstorage.service.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FreezerWeeklyLogData {
    private String weekPeriod; // e.g., "Week 1: Dec 02-08, 2025"
    private String monthYear; // e.g., "December 2025" - for grouping
    private Integer weekNumber; // 1-5 for sorting within month
    private Integer year; // For sorting
    private Integer month; // 1-12 for sorting
    private Integer readingCount; // Number of readings in this week
    private BigDecimal avgTemperature;
    private BigDecimal minTemperature;
    private BigDecimal maxTemperature;
    private BigDecimal avgHumidity;
    private Integer normalCount;
    private Integer warningCount;
    private Integer criticalCount;
    private Integer alertCount;

    // Fields for template compatibility (not used in weekly report, but template
    // declares them)
    private String recordedAt;
    private String date;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private String status;
    private Boolean alertTriggered;
    private Integer daysMonitored;
}
