package org.openelisglobal.notebook.form;

import java.util.List;
import java.util.Map;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Form bean for analyzer result import operations.
 */
public class AnalyzerImportForm {

    private Integer notebookPageId;
    private Integer analyzerId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String fileName;

    private Map<String, String> columnMapping;

    // For well coordinate matching
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String wellCoordinateColumn;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String sampleIdColumn;

    // Assay run metadata (US5 requirements)
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String assayRunId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String operatorId;

    private Map<String, String> machineParameters;

    private List<String> reagentLots;

    public Integer getNotebookPageId() {
        return notebookPageId;
    }

    public void setNotebookPageId(Integer notebookPageId) {
        this.notebookPageId = notebookPageId;
    }

    public Integer getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(Integer analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, String> getColumnMapping() {
        return columnMapping;
    }

    public void setColumnMapping(Map<String, String> columnMapping) {
        this.columnMapping = columnMapping;
    }

    public String getWellCoordinateColumn() {
        return wellCoordinateColumn;
    }

    public void setWellCoordinateColumn(String wellCoordinateColumn) {
        this.wellCoordinateColumn = wellCoordinateColumn;
    }

    public String getSampleIdColumn() {
        return sampleIdColumn;
    }

    public void setSampleIdColumn(String sampleIdColumn) {
        this.sampleIdColumn = sampleIdColumn;
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
        return machineParameters;
    }

    public void setMachineParameters(Map<String, String> machineParameters) {
        this.machineParameters = machineParameters;
    }

    public List<String> getReagentLots() {
        return reagentLots;
    }

    public void setReagentLots(List<String> reagentLots) {
        this.reagentLots = reagentLots;
    }
}
