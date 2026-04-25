package org.openelisglobal.analyzerqc.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A single manual QC run recorded against an analyzer.
 *
 * ID strategy: UUID assigned in @PrePersist — matches AnalyzerQcRule pattern.
 * No schema prefix: the QC rule table has no schema prefix either.
 *
 * Issue #3490 — Analyzer Manual QC Recording
 */
@Entity
@Table(name = "analyzer_qc_run")
public class AnalyzerQcRun extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    /**
     * The analyzer this QC run belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzer_id", nullable = false)
    private Analyzer analyzer;

    /**
     * Pass/Fail outcome. Stored as VARCHAR: "PASS" or "FAIL".
     */
    @Column(name = "result", length = 10, nullable = false)
    private String result;

    /**
     * Optional numeric or freetext measurement recorded with the run.
     */
    @Column(name = "value", length = 500)
    private String value;

    /**
     * When the QC was actually performed (not when it was entered).
     * Defaults to now() if the caller does not supply a value.
     */
    @Column(name = "run_date", nullable = false)
    private Timestamp runDate;

    /**
     * Which entry point triggered this recording.
     * One of: ANALYZER_IMPORT, ANALYZER_LIST, WORKPLAN, QC_MODULE
     */
    @Column(name = "source", length = 50, nullable = false)
    private String source;

    /** Called by Hibernate before first persist — generates UUID if not set. */
    @PrePersist
    protected void prePersist() {
        if (id == null || id.trim().isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        if (runDate == null) {
            runDate = new Timestamp(System.currentTimeMillis());
        }
    }

    @Override
    public String getId() { return id; }

    @Override
    public void setId(String id) { this.id = id; }

    public Analyzer getAnalyzer() { return analyzer; }
    public void setAnalyzer(Analyzer analyzer) { this.analyzer = analyzer; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Timestamp getRunDate() { return runDate; }
    public void setRunDate(Timestamp runDate) { this.runDate = runDate; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
