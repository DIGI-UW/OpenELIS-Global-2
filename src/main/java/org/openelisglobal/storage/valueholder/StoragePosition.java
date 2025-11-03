package org.openelisglobal.storage.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.storage.fhir.StorageLocationFhirTransform;

/**
 * StoragePosition entity - Specific location within a rack Maps to FHIR
 * Location resource with occupancy extension
 */
@Entity
@Table(name = "STORAGE_POSITION")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class StoragePosition extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "storage_position_seq")
    @GenericGenerator(name = "storage_position_seq", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "sequence_name", value = "storage_position_seq")
    })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;
    
    @Column(name = "FHIR_UUID", nullable = false, unique = true)
    private UUID fhirUuid;
    
    @Column(name = "COORDINATE", length = 50, nullable = false)
    private String coordinate;
    
    @Column(name = "ROW_INDEX")
    private Integer rowIndex;
    
    @Column(name = "COLUMN_INDEX")
    private Integer columnIndex;
    
    @Column(name = "OCCUPIED", nullable = false)
    private Boolean occupied;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PARENT_RACK_ID", nullable = false)
    private StorageRack parentRack;
    
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "SYS_USER_ID", precision = 10, scale = 0, nullable = false)
    private String sysUserId;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
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

    public Boolean getOccupied() {
        return occupied;
    }

    public void setOccupied(Boolean occupied) {
        this.occupied = occupied;
    }

    public StorageRack getParentRack() {
        return parentRack;
    }

    public void setParentRack(StorageRack parentRack) {
        this.parentRack = parentRack;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
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
