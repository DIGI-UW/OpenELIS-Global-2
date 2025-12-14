package org.openelisglobal.inventory.valueholder.reports;

import java.math.BigDecimal;

/**
 * Report bean (DTO) for Usage Trend reports Represents consumption patterns and
 * trends for inventory items Used with JRBeanCollectionDataSource for
 * JasperReports integration
 */
public class UsageTrendReportData {

    private String itemId;
    private String itemName;
    private String itemType;
    private String unitOfMeasure;
    private String trendIndicator;
    private String peakUsageMonthName;
    private BigDecimal totalConsumed;
    private BigDecimal averageDailyUsage;
    private BigDecimal averageWeeklyUsage;
    private BigDecimal averageMonthlyUsage;
    private BigDecimal peakUsageMonth;
    private Integer transactionCount;

    // Default constructor
    public UsageTrendReportData() {
    }

    // Getters and Setters

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getTrendIndicator() {
        return trendIndicator;
    }

    public void setTrendIndicator(String trendIndicator) {
        this.trendIndicator = trendIndicator;
    }

    public String getPeakUsageMonthName() {
        return peakUsageMonthName;
    }

    public void setPeakUsageMonthName(String peakUsageMonthName) {
        this.peakUsageMonthName = peakUsageMonthName;
    }

    public BigDecimal getTotalConsumed() {
        return totalConsumed;
    }

    public void setTotalConsumed(BigDecimal totalConsumed) {
        this.totalConsumed = totalConsumed;
    }

    public BigDecimal getAverageDailyUsage() {
        return averageDailyUsage;
    }

    public void setAverageDailyUsage(BigDecimal averageDailyUsage) {
        this.averageDailyUsage = averageDailyUsage;
    }

    public BigDecimal getAverageWeeklyUsage() {
        return averageWeeklyUsage;
    }

    public void setAverageWeeklyUsage(BigDecimal averageWeeklyUsage) {
        this.averageWeeklyUsage = averageWeeklyUsage;
    }

    public BigDecimal getAverageMonthlyUsage() {
        return averageMonthlyUsage;
    }

    public void setAverageMonthlyUsage(BigDecimal averageMonthlyUsage) {
        this.averageMonthlyUsage = averageMonthlyUsage;
    }

    public BigDecimal getPeakUsageMonth() {
        return peakUsageMonth;
    }

    public void setPeakUsageMonth(BigDecimal peakUsageMonth) {
        this.peakUsageMonth = peakUsageMonth;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }
}
