package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_usage")
public class InventoryUsage extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @NotNull
    private InventoryItem inventoryItem;

    @ManyToOne
    @JoinColumn(name = "lot_id", nullable = false)
    @NotNull
    private InventoryLot lot;

    @Column(name = "test_result_id", length = 36)
    private String testResultId;

    @Column(name = "analysis_id", length = 36)
    private String analysisId;

    @Column(name = "quantity_used", nullable = false)
    @NotNull
    @Min(1)
    private Double quantityUsed;

    @Column(name = "usage_date", nullable = false)
    @NotNull
    private Timestamp usageDate;

    @Column(name = "performed_by_user", nullable = false)
    @NotNull
    private Integer performedByUser;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (usageDate == null) {
            usageDate = new Timestamp(System.currentTimeMillis());
        }
    }

    // Getters and Setters

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public InventoryItem getInventoryItem() {
        return inventoryItem;
    }

    public void setInventoryItem(InventoryItem inventoryItem) {
        this.inventoryItem = inventoryItem;
    }

    public InventoryLot getLot() {
        return lot;
    }

    public void setLot(InventoryLot lot) {
        this.lot = lot;
    }

    public String getTestResultId() {
        return testResultId;
    }

    public void setTestResultId(String testResultId) {
        this.testResultId = testResultId;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }

    public Double getQuantityUsed() {
        return quantityUsed;
    }

    public void setQuantityUsed(Double quantityUsed) {
        this.quantityUsed = quantityUsed;
    }

    public Timestamp getUsageDate() {
        return usageDate;
    }

    public void setUsageDate(Timestamp usageDate) {
        this.usageDate = usageDate;
    }

    public Integer getPerformedByUser() {
        return performedByUser;
    }

    public void setPerformedByUser(Integer performedByUser) {
        this.performedByUser = performedByUser;
    }
}
