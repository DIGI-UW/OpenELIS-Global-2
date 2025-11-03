package org.openelisglobal.storage.valueholder;

import java.util.UUID;
import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "storage_shelf")
public class StorageShelf extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "storage_shelf_generator")
    @GenericGenerator(name = "storage_shelf_generator", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", 
        parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "storage_shelf_seq"))
    @Column(name = "id")
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "label", nullable = false, length = 100)
    private String label;

    @Column(name = "capacity_limit")
    private Integer capacityLimit;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_device_id", nullable = false)
    private StorageDevice parentDevice;

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

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

    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
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

    public boolean isActive() {
        return active != null && active;
    }

    public StorageDevice getParentDevice() {
        return parentDevice;
    }

    public void setParentDevice(StorageDevice parentDevice) {
        this.parentDevice = parentDevice;
    }
}

