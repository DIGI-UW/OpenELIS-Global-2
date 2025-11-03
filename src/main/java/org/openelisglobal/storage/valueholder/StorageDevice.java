package org.openelisglobal.storage.valueholder;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "storage_device")
public class StorageDevice extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "storage_device_generator")
    @GenericGenerator(name = "storage_device_generator", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", 
        parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "storage_device_seq"))
    @Column(name = "id")
    private String id;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "type", nullable = false, length = 20)
    private String type; // freezer, refrigerator, cabinet, other

    @Column(name = "temperature_setting", precision = 5, scale = 2)
    private BigDecimal temperatureSetting;

    @Column(name = "capacity_limit")
    private Integer capacityLimit;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_room_id", nullable = false)
    private StorageRoom parentRoom;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getTemperatureSetting() {
        return temperatureSetting;
    }

    public void setTemperatureSetting(BigDecimal temperatureSetting) {
        this.temperatureSetting = temperatureSetting;
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

    public StorageRoom getParentRoom() {
        return parentRoom;
    }

    public void setParentRoom(StorageRoom parentRoom) {
        this.parentRoom = parentRoom;
    }
}

