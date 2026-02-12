/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.hibernateConverter.StringListConverter;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer")
@DynamicUpdate
public class Analyzer extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_seq_gen")
    @GenericGenerator(name = "analyzer_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "scrip_id", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String script_id;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "machine_id", length = 20)
    private String machineId;

    @Column(name = "analyzer_type", length = 30)
    private String type;

    @Column(name = "description", length = 60)
    private String description;

    @Column(name = "location", length = 60)
    private String location;

    @Column(name = "is_active", length = 1)
    private boolean active;

    @Column(name = "has_setup_page", length = 1)
    private boolean hasSetupPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_type_id")
    private AnalyzerType analyzerType;

    // --- Configuration fields (merged from analyzer_configuration) ---

    @Column(name = "ip_address", length = 15)
    @Pattern(regexp = "^(\\d{1,3}\\.){3}\\d{1,3}$", message = "Invalid IPv4 address")
    private String ipAddress;

    @Column(name = "port")
    @Min(1)
    @Max(65535)
    private Integer port;

    @Column(name = "protocol_version", length = 20)
    @Enumerated(EnumType.STRING)
    private ProtocolVersion protocolVersion = ProtocolVersion.ASTM_LIS2_A2;

    @Column(name = "test_unit_ids", columnDefinition = "TEXT")
    @Convert(converter = StringListConverter.class)
    private List<String> testUnitIds = new ArrayList<>();

    @Column(name = "status", length = 20)
    @Enumerated(EnumType.STRING)
    private AnalyzerStatus status = AnalyzerStatus.SETUP;

    @Column(name = "identifier_pattern", length = 255)
    private String identifierPattern;

    @Column(name = "last_activated_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastActivatedDate;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getScript_id() {
        return script_id;
    }

    public void setScript_id(String script_id) {
        this.script_id = script_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getMachineId() {
        return machineId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean getHasSetupPage() {
        return hasSetupPage;
    }

    public void setHasSetupPage(boolean hasSetupPage) {
        this.hasSetupPage = hasSetupPage;
    }

    public AnalyzerType getAnalyzerType() {
        return analyzerType;
    }

    public void setAnalyzerType(AnalyzerType analyzerType) {
        this.analyzerType = analyzerType;
    }

    // --- Configuration field accessors (merged from analyzer_configuration) ---

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

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(ProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public List<String> getTestUnitIds() {
        return testUnitIds;
    }

    public void setTestUnitIds(List<String> testUnitIds) {
        this.testUnitIds = testUnitIds != null ? testUnitIds : new ArrayList<>();
    }

    public AnalyzerStatus getStatus() {
        return status;
    }

    public void setStatus(AnalyzerStatus status) {
        this.status = status;
    }

    public String getIdentifierPattern() {
        return identifierPattern;
    }

    public void setIdentifierPattern(String identifierPattern) {
        this.identifierPattern = identifierPattern;
    }

    public Date getLastActivatedDate() {
        return lastActivatedDate;
    }

    public void setLastActivatedDate(Date lastActivatedDate) {
        this.lastActivatedDate = lastActivatedDate;
    }

    /**
     * Enum for analyzer unified status field. Values must match database
     * constraint: INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE,
     * DELETED
     */
    public enum AnalyzerStatus {
        INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE, DELETED
    }
}
