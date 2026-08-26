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

    // OGC-1129 — the result component this staged value belongs to
    // (test_result_component.id). Null = the test's PRIMARY component.
    @Column(name = "component_id", length = 36)
    private String componentId;

    @Column(name = "test_result_type", length = 1)
    private String resultType = "N";

    @Column(name = "complete_date")
    private Timestamp completeDate;

    // OGC-1145 FR-8: staged row held because its test runs on several sample
    // types and the message carried no specimen; the review page's chooser
    // resolves it.
    public static final String IMPORT_ISSUE_AWAITING_SPECIMEN = "awaiting_specimen";
    public static final String IMPORT_ISSUE_UNKNOWN_TEST = "unknown_analyzer_test";
    public static final String IMPORT_ISSUE_TEST_MAPPING_NOT_READY = "test_mapping_not_ready";
    public static final String IMPORT_ISSUE_UNKNOWN_RESULT_VALUE = "unknown_analyzer_result_value";
    public static final String IMPORT_ISSUE_RESULT_MAPPING_NOT_READY = "result_mapping_not_ready";
    public static final String IMPORT_ISSUE_INVALID_RESULT_MAPPING = "invalid_result_mapping";

    @Column(name = "import_issue_reason", length = 200)
    private String importIssueReason;

    @Column(name = "source_message_id", length = 255)
    private String sourceMessageId;

    @Column(name = "source_connection_id", length = 255)
    private String sourceConnectionId;

    @Column(name = "source_profile_id", length = 128)
    private String sourceProfileId;

    @Column(name = "source_profile_revision")
    private Integer sourceProfileRevision;

    @Column(name = "source_protocol", length = 20)
    private String sourceProtocol;

    @Column(name = "source_transport", length = 20)
    private String sourceTransport;

    @Column(name = "raw_test_code", length = 255)
    private String rawTestCode;

    @Column(name = "raw_result_value", columnDefinition = "TEXT")
    private String rawResultValue;

    @Column(name = "result_classification", length = 12)
    private String resultClassification;

    @Column(name = "recognition_mode", length = 12)
    private String recognitionMode;

    @Column(name = "recognition_outcome", length = 20)
    private String recognitionOutcome;

    @Column(name = "recognition_fingerprint", length = 71)
    private String recognitionFingerprint;

    @Column(name = "source_payload", columnDefinition = "TEXT")
    private String sourcePayload;

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

    public String getSourceMessageId() {
        return sourceMessageId;
    }

    public void setSourceMessageId(String sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }

    public String getSourceConnectionId() {
        return sourceConnectionId;
    }

    public void setSourceConnectionId(String sourceConnectionId) {
        this.sourceConnectionId = sourceConnectionId;
    }

    public String getSourceProfileId() {
        return sourceProfileId;
    }

    public void setSourceProfileId(String sourceProfileId) {
        this.sourceProfileId = sourceProfileId;
    }

    public Integer getSourceProfileRevision() {
        return sourceProfileRevision;
    }

    public void setSourceProfileRevision(Integer sourceProfileRevision) {
        this.sourceProfileRevision = sourceProfileRevision;
    }

    public String getSourceProtocol() {
        return sourceProtocol;
    }

    public void setSourceProtocol(String sourceProtocol) {
        this.sourceProtocol = sourceProtocol;
    }

    public String getSourceTransport() {
        return sourceTransport;
    }

    public void setSourceTransport(String sourceTransport) {
        this.sourceTransport = sourceTransport;
    }

    public String getRawTestCode() {
        return rawTestCode;
    }

    public void setRawTestCode(String rawTestCode) {
        this.rawTestCode = rawTestCode;
    }

    public String getRawResultValue() {
        return rawResultValue;
    }

    public void setRawResultValue(String rawResultValue) {
        this.rawResultValue = rawResultValue;
    }

    public String getResultClassification() {
        return resultClassification;
    }

    public void setResultClassification(String resultClassification) {
        this.resultClassification = resultClassification;
    }

    public String getRecognitionMode() {
        return recognitionMode;
    }

    public void setRecognitionMode(String recognitionMode) {
        this.recognitionMode = recognitionMode;
    }

    public String getRecognitionOutcome() {
        return recognitionOutcome;
    }

    public void setRecognitionOutcome(String recognitionOutcome) {
        this.recognitionOutcome = recognitionOutcome;
    }

    public String getRecognitionFingerprint() {
        return recognitionFingerprint;
    }

    public void setRecognitionFingerprint(String recognitionFingerprint) {
        this.recognitionFingerprint = recognitionFingerprint;
    }

    public String getSourcePayload() {
        return sourcePayload;
    }

    public void setSourcePayload(String sourcePayload) {
        this.sourcePayload = sourcePayload;
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

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getResultType() {
        return resultType;
    }
}
