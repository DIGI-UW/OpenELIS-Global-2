package org.openelisglobal.reportqueue.valueholder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One printed version of a per-order report: what was printed (its items), the
 * archived PDF with its SHA-256, and the print events (the issue and any
 * reprints). A legacy version stands for a print recorded before archiving
 * began and carries neither items nor a PDF.
 */
@Entity
@Table(name = "report_version", schema = "clinlims", uniqueConstraints = @UniqueConstraint(name = "uq_report_version_sample_type_version", columnNames = {
        "sample_id", "report_type", "version" }))
public class ReportVersion extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_PATIENT = "PATIENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "sample_id", nullable = false)
    private Long sampleId;

    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType = TYPE_PATIENT;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "amended", nullable = false)
    private boolean amended;

    @Column(name = "amended_count", nullable = false)
    private Integer amendedCount = 0;

    @Column(name = "reissue", nullable = false)
    private boolean reissue;

    @Column(name = "change_summary")
    private String changeSummary;

    @Column(name = "released_count", nullable = false)
    private Integer releasedCount = 0;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount = 0;

    @Column(name = "issued_by_user_id")
    private String issuedByUserId;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "pdf_content")
    private byte[] pdfContent;

    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    @Column(name = "archive_failed", nullable = false)
    private boolean archiveFailed;

    @Column(name = "legacy", nullable = false)
    private boolean legacy;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("id")
    private List<ReportVersionItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("printedAt")
    private List<ReportPrintEvent> printEvents = new ArrayList<>();

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getSampleId() {
        return sampleId;
    }

    public void setSampleId(Long sampleId) {
        this.sampleId = sampleId;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isAmended() {
        return amended;
    }

    public void setAmended(boolean amended) {
        this.amended = amended;
    }

    public Integer getAmendedCount() {
        return amendedCount;
    }

    public void setAmendedCount(Integer amendedCount) {
        this.amendedCount = amendedCount;
    }

    public boolean isReissue() {
        return reissue;
    }

    public void setReissue(boolean reissue) {
        this.reissue = reissue;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public Integer getReleasedCount() {
        return releasedCount;
    }

    public void setReleasedCount(Integer releasedCount) {
        this.releasedCount = releasedCount;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public String getIssuedByUserId() {
        return issuedByUserId;
    }

    public void setIssuedByUserId(String issuedByUserId) {
        this.issuedByUserId = issuedByUserId;
    }

    public OffsetDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(OffsetDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public byte[] getPdfContent() {
        return pdfContent;
    }

    public void setPdfContent(byte[] pdfContent) {
        this.pdfContent = pdfContent;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public boolean isArchiveFailed() {
        return archiveFailed;
    }

    public void setArchiveFailed(boolean archiveFailed) {
        this.archiveFailed = archiveFailed;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }

    public List<ReportVersionItem> getItems() {
        return items;
    }

    public void setItems(List<ReportVersionItem> items) {
        this.items = items;
    }

    public List<ReportPrintEvent> getPrintEvents() {
        return printEvents;
    }

    public void setPrintEvents(List<ReportPrintEvent> printEvents) {
        this.printEvents = printEvents;
    }
}
