package org.openelisglobal.qc.valueholder;

import java.util.Set;

/**
 * OGC-1147 — the controlled vocabulary for a QC outcome that is not a number.
 * RDT control lines read VALID/INVALID; manual quantitative runs carry
 * PASS/FAIL alongside their measured value; an analyzer control that reports an
 * answer is PASS or FAIL against its Test Catalog QC target, with no value.
 * Never encoded as magic numbers in {@code result_value}.
 *
 * <p>
 * The {@code chk_qc_result_source_shape} CHECK enforces only <em>presence</em>
 * (an RDT row has an outcome and no value; anything quantitative has a value).
 * Pairing an outcome with a legal source lives here rather than in the
 * constraint, because OGC-427/428 will add sources with PASS/FAIL semantics and
 * would otherwise have to rewrite the CHECK.
 */
public enum QCQualitativeOutcome {

    /** RDT control line present — the kit worked. */
    VALID(QCSource.RDT),

    /**
     * RDT control line absent — result cannot be reported, repeat the test.
     */
    INVALID(QCSource.RDT),

    /**
     * Manual control within the captured expected value ± uncertainty, or an
     * analyzer control that gave its expected answer.
     */
    PASS(QCSource.MANUAL, QCSource.ASTM),

    /** Outside tolerance, or the wrong answer — raises the QC-fail signal. */
    FAIL(QCSource.MANUAL, QCSource.ASTM);

    private final Set<QCSource> sources;

    QCQualitativeOutcome(QCSource... sources) {
        this.sources = Set.of(sources);
    }

    /** Whether this outcome represents a control the lab must act on. */
    public boolean isFailing() {
        return this == INVALID || this == FAIL;
    }

    /** Whether this outcome may be recorded against the given source. */
    public boolean isValidFor(QCSource candidate) {
        return sources.contains(candidate);
    }
}
