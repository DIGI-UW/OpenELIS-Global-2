package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "micro_whonet_export_run", schema = "clinlims")
public class MicroWhonetExportRun extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "from_date", nullable = false)
    private Date fromDate;

    @Column(name = "to_date", nullable = false)
    private Date toDate;

    @Column(name = "significance_policy", nullable = false, length = 40)
    private String significancePolicy;

    @Column(name = "dedup_policy", nullable = false, length = 40)
    private String dedupPolicy;

    @Column(name = "case_count", nullable = false)
    private int caseCount;

    @Column(name = "isolate_count", nullable = false)
    private int isolateCount;

    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Column(name = "excluded_row_count", nullable = false)
    private int excludedRowCount;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "generated_at", nullable = false)
    private Timestamp generatedAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "generated_by", nullable = false, length = 20)
    private String generatedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getSignificancePolicy() {
        return significancePolicy;
    }

    public void setSignificancePolicy(String significancePolicy) {
        this.significancePolicy = significancePolicy;
    }

    public String getDedupPolicy() {
        return dedupPolicy;
    }

    public void setDedupPolicy(String dedupPolicy) {
        this.dedupPolicy = dedupPolicy;
    }

    public int getCaseCount() {
        return caseCount;
    }

    public void setCaseCount(int caseCount) {
        this.caseCount = caseCount;
    }

    public int getIsolateCount() {
        return isolateCount;
    }

    public void setIsolateCount(int isolateCount) {
        this.isolateCount = isolateCount;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    public int getExcludedRowCount() {
        return excludedRowCount;
    }

    public void setExcludedRowCount(int excludedRowCount) {
        this.excludedRowCount = excludedRowCount;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public void setContentSha256(String contentSha256) {
        this.contentSha256 = contentSha256;
    }

    public Timestamp getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Timestamp generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }
}
