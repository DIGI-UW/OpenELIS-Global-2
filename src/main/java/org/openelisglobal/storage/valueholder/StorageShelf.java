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
 * StorageShelf entity - Storage shelf within a device Maps to FHIR Location
 * resource with physicalType = "co" (container)
 */
@Entity
@Table(name = "STORAGE_SHELF")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class StorageShelf extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "storage_shelf_seq")
    @GenericGenerator(name = "storage_shelf_seq", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "sequence_name", value = "storage_shelf_seq")
    })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;
    
    @Column(name = "FHIR_UUID", nullable = false, unique = true)
    private UUID fhirUuid;
    
    @Column(name = "LABEL", length = 100, nullable = false)
    private String label;
    
    @Column(name = "CAPACITY_LIMIT")
    private Integer capacityLimit;
    
    @Column(name = "ACTIVE", nullable = false)
    private Boolean active;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PARENT_DEVICE_ID", nullable = false)
    private StorageDevice parentDevice;
    
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getCapacityLimit() {
        return capacityLimit;
    }

    public void setCapacityLimit(Integer capacityLimit) {
        this.capacityLimit = capacityLimit;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public StorageDevice getParentDevice() {
        return parentDevice;
    }

    public void setParentDevice(StorageDevice parentDevice) {
        this.parentDevice = parentDevice;
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
