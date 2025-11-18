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
 * StoragePosition entity - Storage location representing the lowest level in
 * the hierarchy for a sample assignment. A position can have at most 5 levels
 * (Room → Device → Shelf → Rack → Position) but at least 2 levels (Room →
 * Device). The position represents where in the hierarchy the sample is
 * assigned. Minimum requirement is device level (room + device); cannot be just
 * a room. Position can be at: device level (2 levels), shelf level (3 levels),
 * rack level (4 levels), or position level (5 levels).
 * 
 * Maps to FHIR Location resource with occupancy extension.
 */
@Entity
@Table(name = "STORAGE_POSITION")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class StoragePosition extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_position_seq")
    @SequenceGenerator(name = "storage_position_seq", sequenceName = "storage_position_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "FHIR_UUID", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "COORDINATE", length = 50, nullable = true)
    private String coordinate;

    @Column(name = "ROW_INDEX")
    private Integer rowIndex;

    @Column(name = "COLUMN_INDEX")
    private Integer columnIndex;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PARENT_DEVICE_ID", nullable = false)
    private StorageDevice parentDevice;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PARENT_SHELF_ID", nullable = true)
    private StorageShelf parentShelf;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PARENT_RACK_ID", nullable = true)
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

    public String getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(String coordinate) {
        this.coordinate = coordinate;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public Integer getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(Integer columnIndex) {
        this.columnIndex = columnIndex;
    }

    public StorageDevice getParentDevice() {
        return parentDevice;
    }

    public void setParentDevice(StorageDevice parentDevice) {
        this.parentDevice = parentDevice;
    }

    public StorageShelf getParentShelf() {
        return parentShelf;
    }

    public void setParentShelf(StorageShelf parentShelf) {
        this.parentShelf = parentShelf;
    }

    public StorageRack getParentRack() {
        return parentRack;
    }

    public void setParentRack(StorageRack parentRack) {
        this.parentRack = parentRack;
    }

    /**
     * Validate hierarchy integrity constraints. - If parent_rack_id is NOT NULL,
     * then parent_shelf_id must also be NOT NULL - If coordinate is NOT NULL, then
     * parent_rack_id must also be NOT NULL
     * 
     * @return true if hierarchy integrity is valid, false otherwise
     */
    public boolean validateHierarchyIntegrity() {
        // Constraint 1: If rack exists, shelf must exist
        if (parentRack != null && parentShelf == null) {
            return false;
        }
        // Constraint 2: If coordinate exists, rack must exist
        if (coordinate != null && !coordinate.isEmpty() && parentRack == null) {
            return false;
        }
        return true;
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
