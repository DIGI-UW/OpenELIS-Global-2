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
 * StorageRack entity - Storage rack/tray on a shelf with optional grid
 * structure Maps to FHIR Location resource with physicalType = "co" (container)
 */
@Entity
@Table(name = "storage_rack")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class StorageRack extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_rack_seq")
    @SequenceGenerator(name = "storage_rack_seq", sequenceName = "storage_rack_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "FHIR_UUID", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "LABEL", length = 100, nullable = false)
    private String label;

    @Column(name = "CODE", length = 10)
    private String code; // Renamed from shortCode per spec Session 2025-11-16 simplification

    @Column(name = "ACTIVE", nullable = false)
    private Boolean active;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PARENT_SHELF_ID", nullable = false)
    private StorageShelf parentShelf;

    @Column(name = "SYS_USER_ID", nullable = false)
    private Integer sysUserId;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
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

    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public StorageShelf getParentShelf() {
        return parentShelf;
    }

    public void setParentShelf(StorageShelf parentShelf) {
        this.parentShelf = parentShelf;
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
}
