package org.openelisglobal.labunit.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "lab_unit_import_log")
public class LabUnitImportLog extends BaseObject<String> {
    private static final long serialVersionUID = 1L;

    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "id", length = 36)
    private String id;

    @jakarta.persistence.Column(name = "import_date", nullable = false)
    private Timestamp importDate;

    @jakarta.persistence.Column(name = "user_id", length = 36)
    private String userId;

    @jakarta.persistence.Column(name = "total_records", nullable = false)
    private Integer totalRecords;

    @jakarta.persistence.Column(name = "success_count", nullable = false)
    private Integer successCount;

    @jakarta.persistence.Column(name = "error_count", nullable = false)
    private Integer errorCount;

    @jakarta.persistence.Column(name = "error_details", length = 2000)
    private String errorDetails;

    public LabUnitImportLog() {
        super();
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Integer getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(Integer totalRecords) {
        this.totalRecords = totalRecords;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}