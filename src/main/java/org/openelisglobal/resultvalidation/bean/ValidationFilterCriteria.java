package org.openelisglobal.resultvalidation.bean;

/**
 * Holds filter criteria for validation result list filtering. Extracted from
 * HTTP request parameters to decouple filtering logic from the controller.
 */
public class ValidationFilterCriteria {

    private String labNumberFrom;
    private String labNumberTo;
    private String dateFrom;
    private String dateTo;
    private String analyzer;
    private String enteredBy;
    private String normal;
    private String flagged;

    public String getLabNumberFrom() {
        return labNumberFrom;
    }

    public void setLabNumberFrom(String labNumberFrom) {
        this.labNumberFrom = labNumberFrom;
    }

    public String getLabNumberTo() {
        return labNumberTo;
    }

    public void setLabNumberTo(String labNumberTo) {
        this.labNumberTo = labNumberTo;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public String getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(String enteredBy) {
        this.enteredBy = enteredBy;
    }

    public String getNormal() {
        return normal;
    }

    public void setNormal(String normal) {
        this.normal = normal;
    }

    public String getFlagged() {
        return flagged;
    }

    public void setFlagged(String flagged) {
        this.flagged = flagged;
    }
}
