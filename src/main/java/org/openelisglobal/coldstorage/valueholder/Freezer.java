package org.openelisglobal.coldstorage.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.storage.valueholder.StorageDevice;

@Getter
@Setter
@Entity
@Table(name = "freezer", indexes = { @Index(name = "idx_freezer_name", columnList = "name", unique = true) })
public class Freezer extends BaseObject<Long> {

    public enum Protocol {
        TCP, RTU
    }

    public enum Parity {
        NONE, EVEN, ODD, MARK, SPACE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "freezer_generator")
    @SequenceGenerator(name = "freezer_generator", sequenceName = "freezer_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", length = 128, nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_device_id", nullable = true)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private StorageDevice storageDevice;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", length = 8, nullable = false)
    private Protocol protocol;

    @Column(name = "host")
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "serial_port")
    private String serialPort;

    @Column(name = "baud_rate")
    private Integer baudRate;

    @Column(name = "data_bits")
    private Integer dataBits;

    @Column(name = "stop_bits")
    private Integer stopBits;

    @Enumerated(EnumType.STRING)
    @Column(name = "parity", length = 8)
    private Parity parity;

    @Column(name = "slave_id", nullable = false)
    private Integer slaveId;

    @Column(name = "temperature_register", nullable = false)
    private Integer temperatureRegister;

    @Column(name = "humidity_register")
    private Integer humidityRegister;

    @Column(name = "temperature_scale")
    private BigDecimal temperatureScale = BigDecimal.ONE;

    @Column(name = "temperature_offset")
    private BigDecimal temperatureOffset = BigDecimal.ZERO;

    @Column(name = "humidity_scale")
    private BigDecimal humidityScale = BigDecimal.ONE;

    @Column(name = "humidity_offset")
    private BigDecimal humidityOffset = BigDecimal.ZERO;

    @Column(name = "target_temperature")
    private BigDecimal targetTemperature;

    @Column(name = "warning_threshold")
    private BigDecimal warningThreshold;

    @Column(name = "critical_threshold")
    private BigDecimal criticalThreshold;

    @Column(name = "polling_interval_seconds")
    private Integer pollingIntervalSeconds = 60;

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "freezer", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnore
    private List<FreezerReading> readings = new ArrayList<>();

    @OneToMany(mappedBy = "freezer", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnore
    private List<FreezerThresholdProfile> thresholdAssignments = new ArrayList<>();

    /**
     * Convenience method to get device type enum from linked StorageDevice.
     *
     * @return device type enum (FREEZER, REFRIGERATOR, etc.) or null if not linked
     */
    @JsonIgnore
    public StorageDevice.DeviceType getLinkedDeviceType() {
        return storageDevice != null ? storageDevice.getTypeEnum() : null;
    }

    /**
     * Convenience method to get device type as string from linked StorageDevice.
     *
     * @return device type string ("freezer", "refrigerator", etc.) or null if not
     *         linked
     */
    @JsonIgnore
    public String getLinkedDeviceTypeString() {
        return storageDevice != null ? storageDevice.getType() : null;
    }

    /**
     * Convenience method to get temperature setting from linked StorageDevice.
     *
     * @return configured temperature setting or null if not linked
     */
    @JsonIgnore
    public BigDecimal getTemperatureSetting() {
        return storageDevice != null ? storageDevice.getTemperatureSetting() : null;
    }

    /**
     * Convenience method to get parent room from linked StorageDevice. Read-only
     * for JSON serialization - use storageDevice for updates.
     *
     * @return parent storage room or null if not linked
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public org.openelisglobal.storage.valueholder.StorageRoom getStorageRoom() {
        return storageDevice != null ? storageDevice.getParentRoom() : null;
    }

    /**
     * Convenience method to get parent room name for JSON serialization. Read-only
     * for JSON serialization - use storageDevice for updates.
     *
     * @return parent storage room name or null if not linked
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getRoom() {
        org.openelisglobal.storage.valueholder.StorageRoom room = getStorageRoom();
        return room != null ? room.getName() : null;
    }
}
