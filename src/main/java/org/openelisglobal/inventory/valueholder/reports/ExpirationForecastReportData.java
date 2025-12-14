package org.openelisglobal.inventory.valueholder.reports;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Report bean (DTO) for Expiration Forecast reports
 * Represents inventory lots nearing expiration with status indicators
 * Used with JRBeanCollectionDataSource for JasperReports integration
 */
public class ExpirationForecastReportData {

    private String lotId;
    private String lotNumber;
    private String itemName;
    private String itemType;
    private String storageLocationPath;
    private String unitOfMeasure;
    private String expirationStatus;
    private String manufacturer;
    private BigDecimal quantity;
    private LocalDate expirationDate;
    private LocalDate receivedDate;
    private Integer daysUntilExpiration;

    // Default constructor
    public ExpirationForecastReportData() {
    }

    // Getters and Setters

    public String getLotId() {
        return lotId;
    }

    public void setLotId(String lotId) {
        this.lotId = lotId;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
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

    public String getExpirationStatus() {
        return expirationStatus;
    }

    public void setExpirationStatus(String expirationStatus) {
        this.expirationStatus = expirationStatus;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDate getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDate receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Integer getDaysUntilExpiration() {
        return daysUntilExpiration;
    }

    public void setDaysUntilExpiration(Integer daysUntilExpiration) {
        this.daysUntilExpiration = daysUntilExpiration;
    }
}
