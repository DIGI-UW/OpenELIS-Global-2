package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Date;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Auto-detected unmapped analyzer codes queue (FR-021). Status enum,
 * seen_count, sample_payload.
 */
@Entity
@Table(name = "astm_pending_code")
public class AstmPendingCode extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public enum Status {
        PENDING, RESOLVED, IGNORED
    }

    @Id
    @Column(name = "id", length = 36, nullable = false)
    private String id;

    @ManyToOne
    @JoinColumn(name = "analyzer_id", nullable = false)
    @NotNull
    private Analyzer analyzer;

    @Column(name = "analyzer_test_name", nullable = false, length = 120)
    @NotNull
    @Size(max = 120)
    private String analyzerTestName;

    @Column(name = "first_seen_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date lastSeenAt;

    @Column(name = "seen_count", nullable = false)
    @NotNull
    private Integer seenCount = 1;

    @Column(name = "sample_payload", columnDefinition = "TEXT")
    private String samplePayload;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull
    @Size(max = 20)
    private String status = Status.PENDING.name();

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (firstSeenAt == null) {
            firstSeenAt = new Date();
        }
        if (lastSeenAt == null) {
            lastSeenAt = new Date();
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public String getAnalyzerTestName() {
        return analyzerTestName;
    }

    public void setAnalyzerTestName(String analyzerTestName) {
        this.analyzerTestName = analyzerTestName;
    }

    public Date getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Date firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Date getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Date lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Integer getSeenCount() {
        return seenCount;
    }

    public void setSeenCount(Integer seenCount) {
        this.seenCount = seenCount;
    }

    public String getSamplePayload() {
        return samplePayload;
    }

    public void setSamplePayload(String samplePayload) {
        this.samplePayload = samplePayload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
