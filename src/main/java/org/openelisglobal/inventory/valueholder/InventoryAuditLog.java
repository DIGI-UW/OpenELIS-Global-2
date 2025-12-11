package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import org.hibernate.annotations.Immutable;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Immutable audit log for all inventory management operations. Records all
 * changes with full before/after state and operation details. Pattern:
 * Insert-only, never UPDATE or DELETE.
 *
 * Note: Extends BaseObject for DAO compatibility. The last_updated column from
 * BaseObject is used for version tracking (even though audit logs are
 * immutable).
 */
@Entity
@Table(name = "inventory_audit_log")
@Immutable
public class InventoryAuditLog extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_audit_log_generator")
    @SequenceGenerator(name = "inventory_audit_log_generator", sequenceName = "inventory_audit_log_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "timestamp", nullable = false)
    @NotNull
    private Timestamp timestamp;

    @Column(name = "performed_by_user")
    private Integer performedByUser;

    @Column(name = "operation_type", nullable = false, length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @Column(name = "entity_type", nullable = false, length = 50)
    @NotNull
    @Enumerated(EnumType.STRING)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false)
    @NotNull
    private Long entityId;

    @Column(name = "related_entity_type", length = 50)
    @Enumerated(EnumType.STRING)
    private EntityType relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState;

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState;

    @Column(name = "operation_details", columnDefinition = "TEXT")
    private String operationDetails;

    // Denormalized fields for quick queries
    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "item_name", length = 255)
    private String itemName;

    @Column(name = "lot_id")
    private Long lotId;

    @Column(name = "lot_number", length = 100)
    private String lotNumber;

    @Column(name = "location_id")
    private Long locationId;

    // Enums
    public enum OperationType {
        // Item operations
        ITEM_CREATE, ITEM_UPDATE, ITEM_DEACTIVATE, ITEM_REACTIVATE,

        // Lot operations
        LOT_RECEIVE, LOT_OPEN, LOT_QC_UPDATE, LOT_STATUS_UPDATE, LOT_ADJUST, LOT_DISPOSE, LOT_UPDATE,

        // Usage operations
        USAGE_RECORD,

        // Location operations
        LOCATION_CREATE, LOCATION_UPDATE, LOCATION_DEACTIVATE,

        // Transaction operations
        TRANSACTION_CREATE
    }

    public enum EntityType {
        ITEM, LOT, LOCATION, USAGE, TRANSACTION
    }

    // Getters and Setters
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getPerformedByUser() {
        return performedByUser;
    }

    public void setPerformedByUser(Integer performedByUser) {
        this.performedByUser = performedByUser;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public EntityType getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(EntityType relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public String getBeforeState() {
        return beforeState;
    }

    public void setBeforeState(String beforeState) {
        this.beforeState = beforeState;
    }

    public String getAfterState() {
        return afterState;
    }

    public void setAfterState(String afterState) {
        this.afterState = afterState;
    }

    public String getOperationDetails() {
        return operationDetails;
    }

    public void setOperationDetails(String operationDetails) {
        this.operationDetails = operationDetails;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public Long getLotId() {
        return lotId;
    }

    public void setLotId(Long lotId) {
        this.lotId = lotId;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }
}
