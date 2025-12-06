package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ReferenceType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;

@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_transaction")
public class InventoryTransaction extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "lot_id", nullable = false)
    @NotNull
    private InventoryLot lot;

    @Column(name = "transaction_type", nullable = false, length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "quantity_change", nullable = false, precision = 10, scale = 2)
    @NotNull
    private Double quantityChange;

    @Column(name = "quantity_after", nullable = false, precision = 10, scale = 2)
    @NotNull
    private Double quantityAfter;

    @Column(name = "transaction_date", nullable = false)
    @NotNull
    private Timestamp transactionDate;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "reference_type", length = 50)
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "performed_by_user", nullable = false)
    @NotNull
    private Integer performedByUser;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (transactionDate == null) {
            transactionDate = new Timestamp(System.currentTimeMillis());
        }
    }

    // Business logic helper methods

    /**
     * Check if this is a consumption transaction (negative quantity change)
     */
    public boolean isConsumption() {
        return transactionType == TransactionType.CONSUMPTION && quantityChange < 0;
    }

    /**
     * Check if this is a receipt transaction (positive quantity change)
     */
    public boolean isReceipt() {
        return transactionType == TransactionType.RECEIPT && quantityChange > 0;
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

    public InventoryLot getLot() {
        return lot;
    }

    public void setLot(InventoryLot lot) {
        this.lot = lot;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public Double getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(Double quantityChange) {
        this.quantityChange = quantityChange;
    }

    public Double getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(Double quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public Timestamp getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Timestamp transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public ReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(ReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPerformedByUser() {
        return performedByUser;
    }

    public void setPerformedByUser(Integer performedByUser) {
        this.performedByUser = performedByUser;
    }
}
