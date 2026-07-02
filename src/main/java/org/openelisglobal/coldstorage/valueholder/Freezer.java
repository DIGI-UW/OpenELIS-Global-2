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

    /**
     * Byte/word order used to decode multi-byte Modbus register values.
     * {@code BIG_ENDIAN} (the historical, still-default behavior) means the most
     * significant byte/word comes first on the wire; {@code LITTLE_ENDIAN} means
     * least significant first. Real-world devices (e.g. DIY ESP32+DS18B20 Modbus
     * slaves, see issue #3743) do not all agree on this.
     */
    public enum WordOrder {
        BIG_ENDIAN, LITTLE_ENDIAN
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

    /**
     * Number of consecutive 16-bit holding registers to read for a single value (1
     * or 2). Defaults to 1 (a single signed 16-bit register), matching the decoding
     * behavior of every existing configured device. A 32-bit sensor value needs 2.
     */
    @Column(name = "register_count", nullable = false)
    private Integer registerCount = 1;

    /**
     * Byte/word order used when {@link #registerCount} is 2. Ignored for a
     * single-register read. Defaults to {@link WordOrder#BIG_ENDIAN}, matching
     * existing decoding behavior.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "word_order", length = 16, nullable = false)
    private WordOrder wordOrder = WordOrder.BIG_ENDIAN;

    /**
     * RS-485 half-duplex mode for the RTU serial transport (driver-enable/DE
     * timing). Off by default so existing RS-232/point-to-point serial devices are
     * unaffected. See digitalpetri modbus-serial 2.1.5
     * {@code SerialPortTransportConfig} rs485* fields (Linux
     * {@code setRs485ModeParameters}).
     */
    @Column(name = "rs485_mode", nullable = false)
    private Boolean rs485Mode = Boolean.FALSE;

    /**
     * RS-485 RTS polarity: true = RTS high during transmit. Ignored unless
     * {@link #rs485Mode} is set.
     */
    @Column(name = "rs485_rts_active_high", nullable = false)
    private Boolean rs485RtsActiveHigh = Boolean.FALSE;

    /** RS-485 bus termination enabled. Ignored unless {@link #rs485Mode} is set. */
    @Column(name = "rs485_termination", nullable = false)
    private Boolean rs485Termination = Boolean.FALSE;

    /**
     * RS-485: whether the receiver stays enabled while transmitting (echo). Ignored
     * unless {@link #rs485Mode} is set.
     */
    @Column(name = "rs485_rx_during_tx", nullable = false)
    private Boolean rs485RxDuringTx = Boolean.FALSE;

    /**
     * RS-485 driver-enable turnaround delay before transmit, in milliseconds.
     * Ignored unless {@link #rs485Mode} is set.
     */
    @Column(name = "rs485_delay_before_ms", nullable = false)
    private Integer rs485DelayBeforeMs = 0;

    /**
     * RS-485 driver-enable turnaround delay after transmit, in milliseconds.
     * Ignored unless {@link #rs485Mode} is set.
     */
    @Column(name = "rs485_delay_after_ms", nullable = false)
    private Integer rs485DelayAfterMs = 0;

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

    /**
     * Soft-delete marker, distinct from {@link #active}. {@code active} is the
     * operator enable/disable toggle; {@code deleted} means the device was removed
     * and must never be re-activated or reappear in device lists. Reading history
     * is kept (freezer_reading FKs to this row) rather than hard-deleting.
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

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
