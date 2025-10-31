package org.openelisglobal.storage.valueholder;

import java.util.UUID;
import jakarta.persistence.PrePersist;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * StorageRoom entity - Top-level physical location (laboratory room)
 * Maps to FHIR Location resource with physicalType = "ro" (room)
 */
public class StorageRoom extends BaseObject<String> {

    private String id;
    private UUID fhirUuid;
    private String name;
    private String code;
    private String description;
    private Boolean active;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
    }

    // Helper methods for FHIR transform
    public boolean isActive() {
        return active != null && active;
    }

    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }
}
