package org.openelisglobal.reportqueue.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One print of a version: its first print is the issue, every later one a
 * reprint.
 */
@Entity
@Table(name = "report_print_event", schema = "clinlims")
public class ReportPrintEvent extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_version_id", nullable = false)
    private ReportVersion version;

    @Column(name = "printed_at", nullable = false)
    private OffsetDateTime printedAt;

    @Column(name = "printed_by_user_id")
    private String printedByUserId;

    @Column(name = "reprint", nullable = false)
    private boolean reprint;

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public ReportVersion getVersion() {
        return version;
    }

    public void setVersion(ReportVersion version) {
        this.version = version;
    }

    public OffsetDateTime getPrintedAt() {
        return printedAt;
    }

    public void setPrintedAt(OffsetDateTime printedAt) {
        this.printedAt = printedAt;
    }

    public String getPrintedByUserId() {
        return printedByUserId;
    }

    public void setPrintedByUserId(String printedByUserId) {
        this.printedByUserId = printedByUserId;
    }

    public boolean isReprint() {
        return reprint;
    }

    public void setReprint(boolean reprint) {
        this.reprint = reprint;
    }
}
