package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.hibernateConverter.StringListConverter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * AnalyzerConfiguration entity - Extends existing Analyzer entity with
 * connection configuration (IP address, port, protocol version) without
 * modifying legacy XML mappings.
 * 
 * One-to-one relationship with legacy Analyzer entity.
 */
@Entity
@Table(name = "analyzer_configuration")
public class AnalyzerConfiguration extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @OneToOne
    @JoinColumn(name = "analyzer_id", nullable = false, unique = true, referencedColumnName = "id")
    private Analyzer analyzer;

    @Column(name = "ip_address", length = 15)
    @Pattern(regexp = "^(\\d{1,3}\\.){3}\\d{1,3}$", message = "Invalid IPv4 address")
    private String ipAddress;

    @Column(name = "port")
    @Min(1)
    @Max(65535)
    private Integer port;

    @Column(name = "protocol_version", length = 20, nullable = false)
    private String protocolVersion = "ASTM LIS2-A2";

    @Column(name = "test_unit_ids", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> testUnitIds = new ArrayList<>();

    @Column(name = "lifecycle_stage", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private LifecycleStage lifecycleStage = LifecycleStage.SETUP;

    @Column(name = "last_activated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActivatedDate;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
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

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public List<String> getTestUnitIds() {
        return testUnitIds;
    }

    public void setTestUnitIds(List<String> testUnitIds) {
        this.testUnitIds = testUnitIds != null ? testUnitIds : new ArrayList<>();
    }

    public LifecycleStage getLifecycleStage() {
        return lifecycleStage;
    }

    public void setLifecycleStage(LifecycleStage lifecycleStage) {
        this.lifecycleStage = lifecycleStage;
    }

    public Date getLastActivatedDate() {
        return lastActivatedDate;
    }

    public void setLastActivatedDate(Date lastActivatedDate) {
        this.lastActivatedDate = lastActivatedDate;
    }

    /**
     * Enum for analyzer lifecycle stages
     */
    public enum LifecycleStage {
        SETUP, // Initial configuration
        VALIDATION, // Testing and validation
        GO_LIVE, // Active and operational
        MAINTENANCE // Ongoing maintenance
    }
}
