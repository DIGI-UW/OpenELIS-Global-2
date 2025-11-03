package org.openelisglobal.storage.valueholder;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.openelisglobal.common.valueholder.BaseObject;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "storage_room")
public class StorageRoom extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "storage_room_generator")
    @GenericGenerator(name = "storage_room_generator", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", 
        parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "storage_room_seq"))
    @Column(name = "id")
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

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

    public boolean isActive() {
        return active != null && active;
    }
}

