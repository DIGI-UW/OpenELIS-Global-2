package org.openelisglobal.analyzer.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "analyzer_pending_code")
public class AnalyzerPendingCode extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    public enum Status {
        PENDING, MAPPED, IGNORED
    }

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "analyzer_id", nullable = false)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String analyzerId;

    @Column(name = "analyzer_test_name", nullable = false, length = 120)
    private String analyzerTestName;

    @Column(name = "first_seen_at", nullable = false)
    private Timestamp firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Timestamp lastSeenAt;

    @Column(name = "seen_count", nullable = false)
    private Integer seenCount = 1;

    @Column(name = "sample_payload")
    private String samplePayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.PENDING;

    @PrePersist
    protected void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (id == null || id.trim().isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        if (firstSeenAt == null) {
            firstSeenAt = now;
        }
        if (lastSeenAt == null) {
            lastSeenAt = now;
        }
        if (seenCount == null || seenCount < 1) {
            seenCount = 1;
        }
        if (status == null) {
            status = Status.PENDING;
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

    public String getAnalyzerId() {
        return analyzerId;
    }

    public void setAnalyzerId(String analyzerId) {
        this.analyzerId = analyzerId;
    }

    public String getAnalyzerTestName() {
        return analyzerTestName;
    }

    public void setAnalyzerTestName(String analyzerTestName) {
        this.analyzerTestName = analyzerTestName;
    }

    public Timestamp getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Timestamp firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Timestamp getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Timestamp lastSeenAt) {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
