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
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.storage.fhir.StorageLocationFhirTransform;

/**
 * StorageDevice entity - Storage equipment (freezers, refrigerators, cabinets)
 * Maps to FHIR Location resource with physicalType = "ve" (vehicle/equipment)
 */
@Entity
@Table(name = "STORAGE_DEVICE")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class StorageDevice extends BaseObject<Integer> {

    public enum DeviceType {
        FREEZER("freezer"), REFRIGERATOR("refrigerator"), CABINET("cabinet"), OTHER("other");

        private final String value;

        DeviceType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DeviceType fromValue(String value) {
            for (DeviceType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid device type: " + value);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "storage_device_seq")
    @SequenceGenerator(name = "storage_device_seq", sequenceName = "storage_device_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "FHIR_UUID", nullable = false, unique = true)
    private UUID fhirUuid;

    @Column(name = "NAME", length = 255, nullable = false)
    private String name;

    @Column(name = "CODE", length = 50, nullable = false)
    private String code;

    @Column(name = "TYPE", length = 20, nullable = false)
    private String type; // Stored as String in DB, use getTypeEnum() and setTypeEnum() for enum access

    @Column(name = "TEMPERATURE_SETTING", precision = 5, scale = 2)
    private BigDecimal temperatureSetting;

    @Column(name = "CAPACITY_LIMIT")
    private Integer capacityLimit;

    @Column(name = "SHORT_CODE", length = 10)
    private String shortCode;

    @Column(name = "ACTIVE", nullable = false)
    private Boolean active;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PARENT_ROOM_ID", nullable = false)
    private StorageRoom parentRoom;

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

    public DeviceType getTypeEnum() {
        return type != null ? DeviceType.fromValue(type) : null;
    }

    public void setTypeEnum(DeviceType typeEnum) {
        this.type = typeEnum != null ? typeEnum.getValue() : null;
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

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public StorageRoom getParentRoom() {
        return parentRoom;
    }

    public void setParentRoom(StorageRoom parentRoom) {
        this.parentRoom = parentRoom;
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

    public String getTypeAsString() {
        return type; // type is already a String
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
