package org.openelisglobal.configuration.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A name a catalog import could not match to a record: an unknown specimen, lab
 * unit or test, an ambiguous or duplicate name. It waits in the "Needs your
 * decision" queue until someone points it at an existing record (optionally
 * remembering the spelling as an alias) or skips it. One open item exists per
 * kind and spelling, however many rows or runs tripped over it.
 */
@Entity
@Table(name = "unresolved_reference", schema = "clinlims")
public class UnresolvedReference extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_TEST = "TEST";
    public static final String TYPE_SAMPLE_TYPE = "SAMPLE_TYPE";
    public static final String TYPE_TEST_SECTION = "TEST_SECTION";
    public static final String TYPE_UNIT_OF_MEASURE = "UNIT_OF_MEASURE";
    public static final String TYPE_RESULT_COMPONENT = "RESULT_COMPONENT";
    public static final String TYPE_DICTIONARY = "DICTIONARY";

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    public static final String RESOLUTION_USE_EXISTING = "USE_EXISTING";
    public static final String RESOLUTION_ALIAS = "ALIAS";
    public static final String RESOLUTION_SKIP = "SKIP";

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "import_run_id", length = 36)
    private String importRunId;

    @Column(name = "domain", nullable = false, length = 40)
    private String domain;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "reference_type", nullable = false, length = 30)
    private String referenceType;

    @Column(name = "reference_value", nullable = false, length = 255)
    private String referenceValue;

    @Column(name = "context", length = 1000)
    private String context;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_OPEN;

    @Column(name = "resolution", length = 20)
    private String resolution;

    @Column(name = "resolved_target_id", length = 36)
    private String resolvedTargetId;

    @Column(name = "resolved_at")
    private Timestamp resolvedAt;

    @Column(name = "occurrences", nullable = false)
    private int occurrences = 1;

    @Column(name = "sys_user_id", precision = 10, scale = 0)
    private Integer systemUserId;

    public UnresolvedReference() {
        super();
        this.id = UUID.randomUUID().toString();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getImportRunId() {
        return importRunId;
    }

    public void setImportRunId(String importRunId) {
        this.importRunId = importRunId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceValue() {
        return referenceValue;
    }

    public void setReferenceValue(String referenceValue) {
        this.referenceValue = referenceValue;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getResolvedTargetId() {
        return resolvedTargetId;
    }

    public void setResolvedTargetId(String resolvedTargetId) {
        this.resolvedTargetId = resolvedTargetId;
    }

    public Timestamp getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Timestamp resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
