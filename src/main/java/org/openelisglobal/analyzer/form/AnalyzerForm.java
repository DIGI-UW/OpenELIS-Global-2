package org.openelisglobal.analyzer.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Form object for Analyzer entity - used for REST API input validation
 * Following OpenELIS pattern: Form objects for transport, entities for
 * persistence.
 *
 * <p>
 * {@code ignoreUnknown=true} so older frontend builds that still send removed
 * fields (e.g. {@code archiveDirectory} / {@code errorDirectory} dropped in
 * Liquibase 012) don't break the POST — defensive forward-compat against
 * frontend/backend version drift.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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

    private String defaultConfigId; // Transient: e.g. "astm/genexpert-astm" — hints controller to auto-create test
                                    // mappings

    private String communicationMode; // ANALYZER_INITIATED, LIS_INITIATED, BOTH (nullable = infer from protocol)

    private String importDirectory;

    private String filePattern;

    private String fileFormat;

    private Map<String, String> columnMappings;

    private String delimiter;

    private Boolean hasHeader;

    private Integer skipRows;

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

    public String getDefaultConfigId() {
        return defaultConfigId;
    }

    public void setDefaultConfigId(String defaultConfigId) {
        this.defaultConfigId = defaultConfigId;
    }

    public String getCommunicationMode() {
        return communicationMode;
    }

    public void setCommunicationMode(String communicationMode) {
        this.communicationMode = communicationMode;
    }

    public String getImportDirectory() {
        return importDirectory;
    }

    public void setImportDirectory(String importDirectory) {
        this.importDirectory = importDirectory;
    }

    public String getFilePattern() {
        return filePattern;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public Map<String, String> getColumnMappings() {
        return columnMappings;
    }

    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings = columnMappings;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public Boolean getHasHeader() {
        return hasHeader;
    }

    public void setHasHeader(Boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    public Integer getSkipRows() {
        return skipRows;
    }

    public void setSkipRows(Integer skipRows) {
        this.skipRows = skipRows;
    }
}
