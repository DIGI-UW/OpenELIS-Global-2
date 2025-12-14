package org.openelisglobal.inventory.valueholder.reports;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Report bean (DTO) for Stock Level reports
 * Represents current inventory stock levels for a single item
 * Used with JRBeanCollectionDataSource for JasperReports integration
 */
public class StockLevelReportData {

    private String itemId;
    private String itemName;
    private String itemType;
    private String categoryName;
    private String storageLocationPath;
    private String storageLocationId;
    private BigDecimal totalQuantity;
    private String unitOfMeasure;
    private BigDecimal reorderLevel;
    private Boolean isBelowReorderLevel;
    private Integer activeLotCount;
    private LocalDateTime lastUpdated;

    // Default constructor
    public StockLevelReportData() {
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getStorageLocationPath() {
        return storageLocationPath;
    }

    public void setStorageLocationPath(String storageLocationPath) {
        this.storageLocationPath = storageLocationPath;
    }

    public String getStorageLocationId() {
        return storageLocationId;
    }

    public void setStorageLocationId(String storageLocationId) {
        this.storageLocationId = storageLocationId;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public BigDecimal getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(BigDecimal reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Boolean getIsBelowReorderLevel() {
        return isBelowReorderLevel;
    }

    public void setIsBelowReorderLevel(Boolean isBelowReorderLevel) {
        this.isBelowReorderLevel = isBelowReorderLevel;
    }

    public Integer getActiveLotCount() {
        return activeLotCount;
    }

    public void setActiveLotCount(Integer activeLotCount) {
        this.activeLotCount = activeLotCount;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
