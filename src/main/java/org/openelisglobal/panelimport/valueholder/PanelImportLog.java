package org.openelisglobal.panelimport.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

public class PanelImportLog extends BaseObject<String> {

    private String id;
    private Timestamp importDate;
    private String importedBy;
    private String fileName;
    private Integer panelsCreated;
    private Integer panelsUpdated;
    private Integer panelsSkipped;
    private String warnings;
    private String importData;

    public PanelImportLog() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getImportDate() {
        return importDate;
    }

    public void setImportDate(Timestamp importDate) {
        this.importDate = importDate;
    }

    public String getImportedBy() {
        return importedBy;
    }

    public void setImportedBy(String importedBy) {
        this.importedBy = importedBy;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getPanelsCreated() {
        return panelsCreated;
    }

    public void setPanelsCreated(Integer panelsCreated) {
        this.panelsCreated = panelsCreated;
    }

    public Integer getPanelsUpdated() {
        return panelsUpdated;
    }

    public void setPanelsUpdated(Integer panelsUpdated) {
        this.panelsUpdated = panelsUpdated;
    }

    public Integer getPanelsSkipped() {
        return panelsSkipped;
    }

    public void setPanelsSkipped(Integer panelsSkipped) {
        this.panelsSkipped = panelsSkipped;
    }

    public String getWarnings() {
        return warnings;
    }

    public void setWarnings(String warnings) {
        this.warnings = warnings;
    }

    public String getImportData() {
        return importData;
    }

    public void setImportData(String importData) {
        this.importData = importData;
    }

}
