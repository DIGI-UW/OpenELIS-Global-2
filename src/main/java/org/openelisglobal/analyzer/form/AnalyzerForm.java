package org.openelisglobal.analyzer.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Form object for Analyzer entity - used for REST API input validation
 * Following OpenELIS pattern: Form objects for transport, entities for
 * persistence
 */
public class AnalyzerForm {

    private String id;

    @NotBlank(message = "Analyzer name is required")
    @Size(min = 1, max = 100, message = "Analyzer name must be between 1 and 100 characters")
    private String name;

    @NotBlank(message = "Analyzer type is required")
    private String analyzerType;

    private String ipAddress; // Optional - validated in controller if provided

    private Integer port; // Optional - validated in controller if provided (1-65535)

    private String protocolVersion = "ASTM LIS2-A2";

    private List<String> testUnitIds;

    private String status; // Unified status: INACTIVE, SETUP, VALIDATION, ACTIVE, ERROR_PENDING, OFFLINE

    private String identifierPattern; // For generic plugin: regex to match message identifier

    private String pluginTypeId; // FK to analyzer_type table (the plugin that handles messages)

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAnalyzerType() {
        return analyzerType;
    }

    public void setAnalyzerType(String analyzerType) {
        this.analyzerType = analyzerType;
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
        this.testUnitIds = testUnitIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdentifierPattern() {
        return identifierPattern;
    }

    public void setIdentifierPattern(String identifierPattern) {
        this.identifierPattern = identifierPattern;
    }

    public String getPluginTypeId() {
        return pluginTypeId;
    }

    public void setPluginTypeId(String pluginTypeId) {
        this.pluginTypeId = pluginTypeId;
    }
}
