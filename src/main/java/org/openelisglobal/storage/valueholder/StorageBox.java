package org.openelisglobal.storage.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.storage.fhir.StorageLocationFhirTransform;

/**
 * StorageBox entity - Gridded container (e.g., 96-well plate, sample box)
 * within a rack. The grid dimensions (rows × columns) define the internal
 * coordinate system. Sample assignments reference the box ID + coordinate
 * (e.g., "A1", "B3"). Hierarchy: Room → Device → Shelf → Rack → Box (gridded
 * container)
 */
@Entity
@Table(name = "storage_box")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class StorageBox extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_box_seq")
    @SequenceGenerator(name = "storage_box_seq", sequenceName = "storage_box_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "FHIR_UUID", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "LABEL", length = 100, nullable = false)
    private String label;

    @Column(name = "TYPE", length = 50)
    private String type; // e.g., "96-well", "384-well", "9x9"

    @Column(name = "ROWS", nullable = false)
    private Integer rows;

    @Column(name = "COLUMNS", nullable = false)
    private Integer columns;

    @Column(name = "POSITION_SCHEMA_HINT", length = 50)
    private String positionSchemaHint; // e.g., "letter-number" for "A1" format

    @Column(name = "CODE", length = 10, nullable = false)
    private String code;

    @Column(name = "ACTIVE", nullable = false)
    private Boolean active;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PARENT_RACK_ID", nullable = false)
    private StorageRack parentRack;

    @Column(name = "SYS_USER_ID", nullable = false)
    private Integer sysUserId;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getRows() {
        return rows;
    }

    public void setRows(Integer rows) {
        this.rows = rows;
    }

    public Integer getColumns() {
        return columns;
    }

    public void setColumns(Integer columns) {
        this.columns = columns;
    }

    public String getPositionSchemaHint() {
        return positionSchemaHint;
    }

    public void setPositionSchemaHint(String positionSchemaHint) {
        this.positionSchemaHint = positionSchemaHint;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getCapacity() {
        if (rows == null || columns == null || rows == 0 || columns == 0) {
            return 0;
        }
        return rows * columns;
    }

    public StorageRack getParentRack() {
        return parentRack;
    }

    public void setParentRack(StorageRack parentRack) {
        this.parentRack = parentRack;
    }

    /**
     * Validate hierarchy integrity constraints. - A box must always have a parent
     * rack.
     *
     * @return true if hierarchy integrity is valid, false otherwise
     */
    public boolean validateHierarchyIntegrity() {
        return parentRack != null;
    }

    public Integer getSysUserIdValue() {
        return sysUserId;
    }

    public void setSysUserIdValue(Integer sysUserId) {
        this.sysUserId = sysUserId;
    }

    @Override
    public String getSysUserId() {
        return sysUserId != null ? sysUserId.toString() : null;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId != null ? Integer.parseInt(sysUserId) : null;
    }

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Helper methods for FHIR transform
    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }

    @PostPersist
    protected void onPostPersist() {
        syncToFhir(true);
    }

    @PostUpdate
    protected void onPostUpdate() {
        syncToFhir(false);
    }

    private void syncToFhir(boolean isCreate) {
        try {
            StorageLocationFhirTransform transformService = SpringContext.getBean(StorageLocationFhirTransform.class);
            if (transformService != null) {
                transformService.syncToFhir(this, isCreate);
            }
        } catch (Exception e) {
            // Log error but don't fail the transaction
            // Errors are logged in the syncToFhir method
            // In test contexts, SpringContext may not be available - ignore silently
        }
    }
}
