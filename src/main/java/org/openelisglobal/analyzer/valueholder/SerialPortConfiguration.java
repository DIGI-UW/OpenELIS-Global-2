package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * SerialPortConfiguration entity - RS232 serial communication parameters for
 * analyzers.
 * 
 * 
 * One-to-one relationship with legacy Analyzer entity (via analyzer_id). Stores
 * serial port settings: port name, baud rate, data bits, stop bits, parity,
 * flow control.
 */
@Entity
@Table(name = "serial_port_configuration")
public class SerialPortConfiguration extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "analyzer_id", nullable = false, unique = true)
    @NotNull(message = "Analyzer ID is required")
    private Integer analyzerId;

    @Column(name = "port_name", nullable = false, length = 50)
    @NotNull(message = "Port name is required")
    private String portName;

    @Column(name = "baud_rate", nullable = false)
    @Min(value = 9600, message = "Baud rate must be at least 9600")
    @Max(value = 115200, message = "Baud rate must be at most 115200")
    private Integer baudRate = 9600;

    @Column(name = "data_bits", nullable = false)
    @Min(value = 7, message = "Data bits must be 7 or 8")
    @Max(value = 8, message = "Data bits must be 7 or 8")
    private Integer dataBits = 8;

    @Column(name = "stop_bits", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private StopBits stopBits = StopBits.ONE;

    @Column(name = "parity", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Parity parity = Parity.NONE;

    @Column(name = "flow_control", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private FlowControl flowControl = FlowControl.NONE;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
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

    public Integer getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(Integer analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getPortName() {
        return portName;
    }

    public void setPortName(String portName) {
        this.portName = portName;
    }

    public Integer getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(Integer baudRate) {
        this.baudRate = baudRate;
    }

    public Integer getDataBits() {
        return dataBits;
    }

    public void setDataBits(Integer dataBits) {
        this.dataBits = dataBits;
    }

    public StopBits getStopBits() {
        return stopBits;
    }

    public void setStopBits(StopBits stopBits) {
        this.stopBits = stopBits;
    }

    public Parity getParity() {
        return parity;
    }

    public void setParity(Parity parity) {
        this.parity = parity;
    }

    public FlowControl getFlowControl() {
        return flowControl;
    }

    public void setFlowControl(FlowControl flowControl) {
        this.flowControl = flowControl;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }
}
