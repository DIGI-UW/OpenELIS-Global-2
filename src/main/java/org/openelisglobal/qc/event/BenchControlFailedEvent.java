package org.openelisglobal.qc.event;

import org.openelisglobal.qc.valueholder.QCResult;
import org.springframework.context.ApplicationEvent;

/**
 * OGC-1147 — a bench control (manual quantitative or RDT) was recorded as
 * failing.
 *
 * <p>
 * Raised instead of calling the signal machinery inline, because the capture
 * and the signal must not share a transaction. Anything that throws while
 * raising the signal marks the surrounding transaction rollback-only, and no
 * amount of catching inside that transaction undoes it — so an inline call
 * would silently discard the very control result it was reacting to. Handling
 * this after commit means a control that was run is always recorded, even if
 * the downstream signal fails.
 *
 * <p>
 * Distinct from {@link QCResultCreatedEvent}, which drives Westgard rule
 * evaluation and fires only for results carrying a z-score. An RDT control has
 * no z-score and must still raise the signal.
 */
public class BenchControlFailedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final transient QCResult result;

    public BenchControlFailedEvent(Object source, QCResult result) {
        super(source);
        this.result = result;
    }

    public QCResult getResult() {
        return result;
    }
}
