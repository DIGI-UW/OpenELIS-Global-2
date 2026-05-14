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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.analyzerresults.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

/**
 * Stores raw results from analyzer instruments before processing and
 * validation. Uses legacy uppercase table name and mixed-case column
 * conventions.
 */
@Entity
@Table(name = "ANALYZER_RESULTS")
public class AnalyzerResults extends BaseObject<String> implements Cloneable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @GeneratedValue(generator = "analyzer_results_seq_gen")
    @GenericGenerator(name = "analyzer_results_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = @Parameter(name = "sequence_name", value = "analyzer_results_seq"))
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "ANALYZER_ID", precision = 10, scale = 0)
    @Convert(converter = StringToIntegerConverter.class)
    private String analyzerId;

    @Column(name = "ACCESSION_NUMBER", length = 20)
    private String accessionNumber;

    @Column(name = "test_name")
    private String testName;

    @Column(name = "RESULT")
    private String result;

    @Column(name = "UNITS")
    private String units;

    @Column(name = "DUPLICATE_ID", length = 10)
    @Convert(converter = StringToIntegerConverter.class)
    private String duplicateAnalyzerResultId;

    @Column(name = "ISCONTROL", length = 1)
    private boolean isControl = false;

    @Column(name = "read_only", length = 1)
    private boolean isReadOnly = false;

    @Column(name = "test_id")
    @Convert(converter = StringToIntegerConverter.class)
    private String testId;

    @Column(name = "test_result_type", length = 1)
    private String resultType = "N";

    @Column(name = "complete_date")
    private Timestamp completeDate;

    @Column(name = "import_issue_reason", length = 200)
    private String importIssueReason;

    // QC metadata propagated from the analyzer-bridge for control samples.
    // Transient — only carried in-memory from FHIR ingest
    // (AnalyzerFhirImportController) through to QCResultProcessingService.
    // Not persisted on analyzer_results because the matched lot is
    // already recorded on the qc_result row (control_lot_id FK).
    // - lotNumber: canonical qc_control_lot.lot_number when the bridge
    // extracted it (ASTM Q-segment field 3 component 2)
    // - controlLevel: clinical level identifier (LPC/HPC/CNEG/CPOS/etc.)
    // — ASTM Q-segment field 3 component 3, OR matched FILE qcRule's
    // SPECIMEN_ID_PREFIX operand
    @jakarta.persistence.Transient
    private String lotNumber;

    @jakarta.persistence.Transient
    private String controlLevel;

    public String getImportIssueReason() {
        return importIssueReason;
    }

    public void setImportIssueReason(String importIssueReason) {
        this.importIssueReason = importIssueReason;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getControlLevel() {
        return controlLevel;
    }

    public void setControlLevel(String controlLevel) {
        this.controlLevel = controlLevel;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber.replaceAll("\'", "");
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestName() {
        return testName;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getResult() {
        return this.result;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getUnits() {
        return units;
    }

    public void setIsControl(boolean isControl) {
        this.isControl = isControl;
    }

    public boolean getIsControl() {
        return isControl;
    }

    public void setCompleteDate(Timestamp completeDate) {
        this.completeDate = completeDate;
    }

    public Timestamp getCompleteDate() {
        return completeDate;
    }

    public String getCompleteDateForDisplay() {
        return DateUtil.convertTimestampToStringDate(completeDate);
    }

    public void setDuplicateAnalyzerResultId(String duplicateAnalyzerResultId) {
        this.duplicateAnalyzerResultId = duplicateAnalyzerResultId;
    }

    public String getDuplicateAnalyzerResultId() {
        return duplicateAnalyzerResultId;
    }

    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public String getTestId() {
        return testId;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getResultType() {
        return resultType;
    }
}
