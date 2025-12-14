package org.openelisglobal.inventory.valueholder.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Report bean (DTO) for Low Stock reports Represents items below reorder level
 * with shortfall calculations Used with JRBeanCollectionDataSource for
 * JasperReports integration
 */
public class LowStockReportData {

    private String itemId;
    private String itemName;
    private String itemType;
    private String storageLocationPath;
    private String unitOfMeasure;
    private BigDecimal currentQuantity;
    private BigDecimal reorderLevel;
    private BigDecimal shortfall;
    private BigDecimal recommendedOrderQuantity;
    private Integer daysUntilStockout; // nullable
    private LocalDateTime lastUpdated;

    // Default constructor
    public LowStockReportData() {
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

    public String getStorageLocationPath() {
        return storageLocationPath;
    }

    public void setStorageLocationPath(String storageLocationPath) {
        this.storageLocationPath = storageLocationPath;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public BigDecimal getShortfall() {
        return shortfall;
    }

    public void setShortfall(BigDecimal shortfall) {
        this.shortfall = shortfall;
    }

    public BigDecimal getRecommendedOrderQuantity() {
        return recommendedOrderQuantity;
    }

    public void setRecommendedOrderQuantity(BigDecimal recommendedOrderQuantity) {
        this.recommendedOrderQuantity = recommendedOrderQuantity;
    }

    public Integer getDaysUntilStockout() {
        return daysUntilStockout;
    }

    public void setDaysUntilStockout(Integer daysUntilStockout) {
        this.daysUntilStockout = daysUntilStockout;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
