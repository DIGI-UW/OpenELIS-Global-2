package org.openelisglobal.analyzerqc.form;

import java.sql.Timestamp;

/**
 * Request body for POST /rest/analyzers/{id}/qc-runs.
 *
 * Fields:
 *   result            — required: "PASS" or "FAIL"
 *   value             — optional: numeric or freetext measurement
 *   runDate           — optional: defaults to now() if null
 *   performedByUserId — optional: defaults to session user if null
 *   source            — required: "ANALYZER_IMPORT" | "ANALYZER_LIST"
 */
public class QcRunForm {

    private String result;
    private String value;
    private Timestamp runDate;
    private String performedByUserId;
    private String source;

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Timestamp getRunDate() { return runDate; }
    public void setRunDate(Timestamp runDate) { this.runDate = runDate; }

    public String getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(String performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
