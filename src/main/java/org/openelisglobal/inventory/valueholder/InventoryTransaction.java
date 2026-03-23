package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.inventory.valueholder.InventoryEnums.ReferenceType;
import org.openelisglobal.inventory.valueholder.InventoryEnums.TransactionType;

@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_transaction")
public class InventoryTransaction extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_transaction_generator")
    @SequenceGenerator(name = "inventory_transaction_generator", sequenceName = "inventory_transaction_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

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

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_type", length = 50)
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "performed_by_user", nullable = false)
    @NotNull
    private Integer performedByUser;

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
}
