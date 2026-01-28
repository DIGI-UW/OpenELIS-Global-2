package org.openelisglobal.analyzer.valueholder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * FileImportConfiguration entity - Configuration for file-based analyzer result
 * import (directory watching, CSV/TXT parsing).
 * 
 * One-to-one relationship with legacy Analyzer entity (via analyzer_id).
 */
@Entity
@Table(name = "file_import_configuration")
public class FileImportConfiguration extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(generator = "uuid")
    @org.hibernate.annotations.GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @Column(name = "analyzer_id", nullable = false, unique = true)
    private Integer analyzerId; // Manual FK to Analyzer (XML-mapped)

    @Column(name = "import_directory", nullable = false, length = 255)
    private String importDirectory; // e.g., "/data/analyzer-imports/quantstudio"

    @Column(name = "file_pattern", length = 100, nullable = false)
    private String filePattern = "*.csv"; // Glob pattern

    @Column(name = "archive_directory", length = 255)
    private String archiveDirectory; // Move processed files here

    @Column(name = "error_directory", length = 255)
    private String errorDirectory; // Move failed files here

    @Column(name = "column_mappings", columnDefinition = "TEXT")
    private String columnMappingsJson; // JSON string: {"Sample_ID": "sampleId", "Result": "result"}

    @Transient
    private Map<String, String> columnMappings = new HashMap<>(); // Transient for easier access

    @Column(name = "delimiter", length = 10, nullable = false)
    private String delimiter = ","; // CSV delimiter

    @Column(name = "has_header", nullable = false)
    private Boolean hasHeader = true;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
        // Serialize column mappings to JSON before persisting
        if (columnMappings != null && !columnMappings.isEmpty()) {
            try {
                columnMappingsJson = objectMapper.writeValueAsString(columnMappings);
            } catch (JsonProcessingException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "onCreate",
                        "Error serializing column mappings to JSON: " + e.getMessage());
                columnMappingsJson = "{}";
            }
        } else if (columnMappingsJson == null) {
            columnMappingsJson = "{}";
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

    public String getArchiveDirectory() {
        return archiveDirectory;
    }

    public void setArchiveDirectory(String archiveDirectory) {
        this.archiveDirectory = archiveDirectory;
    }

    public String getErrorDirectory() {
        return errorDirectory;
    }

    public void setErrorDirectory(String errorDirectory) {
        this.errorDirectory = errorDirectory;
    }

    public Map<String, String> getColumnMappings() {
        // Deserialize from JSON if needed
        if (columnMappings.isEmpty() && columnMappingsJson != null && !columnMappingsJson.isEmpty()) {
            try {
                columnMappings = objectMapper.readValue(columnMappingsJson, new TypeReference<Map<String, String>>() {
                });
            } catch (JsonProcessingException e) {
                LogEvent.logError(this.getClass().getSimpleName(), "getColumnMappings",
                        "Error deserializing column mappings from JSON: " + e.getMessage());
                columnMappings = new HashMap<>();
            }
        }
        return columnMappings;
    }

    public void setColumnMappings(Map<String, String> columnMappings) {
        this.columnMappings = columnMappings != null ? columnMappings : new HashMap<>();
        // Serialize to JSON immediately
        try {
            columnMappingsJson = objectMapper.writeValueAsString(this.columnMappings);
        } catch (JsonProcessingException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "setColumnMappings",
                    "Error serializing column mappings to JSON: " + e.getMessage());
            columnMappingsJson = "{}";
        }
    }

    public String getColumnMappingsJson() {
        return columnMappingsJson;
    }

    public void setColumnMappingsJson(String columnMappingsJson) {
        this.columnMappingsJson = columnMappingsJson;
        // Clear transient map to force re-deserialization
        this.columnMappings = new HashMap<>();
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
