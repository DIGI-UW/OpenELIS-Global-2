package org.openelisglobal.notebook.valueholder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Audit trail for bulk result imports from laboratory analyzers. Tracks import
 * attempts, success/failure counts, and column mappings.
 */
@Entity
@Table(name = "analyzer_result_import")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class AnalyzerResultImport extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "analyzer_result_import_generator")
    @SequenceGenerator(name = "analyzer_result_import_generator", sequenceName = "analyzer_result_import_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_page_id", nullable = false)
    private NoteBookPage notebookPage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id")
    private Analyzer analyzer;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "import_date", nullable = false)
    private Timestamp importDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imported_by", nullable = false)
    private SystemUser importedBy;

    @Column(name = "total_rows", nullable = false)
    private Integer totalRows;

    @Column(name = "successful_rows", nullable = false)
    private Integer successfulRows;

    @Column(name = "failed_rows", nullable = false)
    private Integer failedRows;

    @Transient
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    @Type(type = "jsonb")
    @Column(name = "column_mapping", columnDefinition = "jsonb")
    private String columnMappingJson;

    @Type(type = "jsonb")
    @Column(name = "error_details", columnDefinition = "jsonb")
    private String errorDetailsJson;

    /** Unique identifier for the assay run (per workflow requirements). */
    @Column(name = "assay_run_id", length = 100)
    private String assayRunId;

    /** Operator who performed the assay (may differ from importer). */
    @Column(name = "operator_id", length = 100)
    private String operatorId;

    /** Machine/instrument parameters as key-value pairs (stored as JSON string). */
    @Type(type = "jsonb")
    @Column(name = "machine_parameters", columnDefinition = "jsonb")
    private String machineParametersJson;

    /** Reagent lot numbers used in the assay (stored as JSON string). */
    @Type(type = "jsonb")
    @Column(name = "reagent_lots", columnDefinition = "jsonb")
    private String reagentLotsJson;

    /** File format of the imported file (CSV, XLSX, XLS). */
    @Column(name = "file_format", length = 20)
    private String fileFormat;

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public NoteBookPage getNotebookPage() {
        return notebookPage;
    }

    public void setNotebookPage(NoteBookPage notebookPage) {
        this.notebookPage = notebookPage;
    }

    public Integer getNotebookPageId() {
        return notebookPage != null ? notebookPage.getId() : null;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Timestamp getImportDate() {
        return importDate;
    }

    public void setImportDate(Timestamp importDate) {
        this.importDate = importDate;
    }

    public SystemUser getImportedBy() {
        return importedBy;
    }

    public void setImportedBy(SystemUser importedBy) {
        this.importedBy = importedBy;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getSuccessfulRows() {
        return successfulRows;
    }

    public void setSuccessfulRows(Integer successfulRows) {
        this.successfulRows = successfulRows;
    }

    public Integer getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(Integer failedRows) {
        this.failedRows = failedRows;
    }

    public Map<String, String> getColumnMapping() {
        if (columnMappingJson == null || columnMappingJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JSON_MAPPER.readValue(columnMappingJson, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    public void setColumnMapping(Map<String, String> columnMapping) {
        if (columnMapping == null) {
            this.columnMappingJson = null;
        } else {
            try {
                this.columnMappingJson = JSON_MAPPER.writeValueAsString(columnMapping);
            } catch (JsonProcessingException e) {
                this.columnMappingJson = null;
            }
        }
    }

    public String getColumnMappingJson() {
        return columnMappingJson;
    }

    public void setColumnMappingJson(String columnMappingJson) {
        this.columnMappingJson = columnMappingJson;
    }

    public List<Map<String, Object>> getErrorDetails() {
        if (errorDetailsJson == null || errorDetailsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON_MAPPER.readValue(errorDetailsJson, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    public void setErrorDetails(List<Map<String, Object>> errorDetails) {
        if (errorDetails == null) {
            this.errorDetailsJson = null;
        } else {
            try {
                this.errorDetailsJson = JSON_MAPPER.writeValueAsString(errorDetails);
            } catch (JsonProcessingException e) {
                this.errorDetailsJson = null;
            }
        }
    }

    public String getErrorDetailsJson() {
        return errorDetailsJson;
    }

    public void setErrorDetailsJson(String errorDetailsJson) {
        this.errorDetailsJson = errorDetailsJson;
    }

    public String getAssayRunId() {
        return assayRunId;
    }

    public void setAssayRunId(String assayRunId) {
        this.assayRunId = assayRunId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public Map<String, String> getMachineParameters() {
        if (machineParametersJson == null || machineParametersJson.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JSON_MAPPER.readValue(machineParametersJson, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }

    public void setMachineParameters(Map<String, String> machineParameters) {
        if (machineParameters == null) {
            this.machineParametersJson = null;
        } else {
            try {
                this.machineParametersJson = JSON_MAPPER.writeValueAsString(machineParameters);
            } catch (JsonProcessingException e) {
                this.machineParametersJson = null;
            }
        }
    }

    public String getMachineParametersJson() {
        return machineParametersJson;
    }

    public void setMachineParametersJson(String machineParametersJson) {
        this.machineParametersJson = machineParametersJson;
    }

    public List<String> getReagentLots() {
        if (reagentLotsJson == null || reagentLotsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON_MAPPER.readValue(reagentLotsJson, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    public void setReagentLots(List<String> reagentLots) {
        if (reagentLots == null) {
            this.reagentLotsJson = null;
        } else {
            try {
                this.reagentLotsJson = JSON_MAPPER.writeValueAsString(reagentLots);
            } catch (JsonProcessingException e) {
                this.reagentLotsJson = null;
            }
        }
    }

    public String getReagentLotsJson() {
        return reagentLotsJson;
    }

    public void setReagentLotsJson(String reagentLotsJson) {
        this.reagentLotsJson = reagentLotsJson;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    /** Returns true if all rows were imported successfully. */
    public boolean isFullySuccessful() {
        return failedRows != null && failedRows == 0;
    }

    /** Returns the success rate as a percentage. */
    public double getSuccessRate() {
        if (totalRows == null || totalRows == 0) {
            return 0.0;
        }
        return (successfulRows != null ? successfulRows : 0) * 100.0 / totalRows;
    }
}
