package org.openelisglobal.qc.event;

import org.openelisglobal.qc.valueholder.QCResult;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when a new QC result is created (T098).
 *
 * This event triggers automatic Westgard rule evaluation and alert generation
 * for any violations detected.
 */
public class QCResultCreatedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final QCResult result;

    /**
     * Create a new QCResultCreatedEvent.
     *
     * @param source The object that published the event
     * @param result The newly created QC result
     */
    public QCResultCreatedEvent(Object source, QCResult result) {
        super(source);
        this.result = result;
    }

    /**
     * Get the QC result that was created.
     *
     * @return The QC result
     */
    public QCResult getResult() {
        return result;
    }

    /**
     * Get the result ID for convenience.
     *
     * @return The result ID
     */
    public String getResultId() {
        return result != null ? result.getId() : null;
    }

    /**
     * Get the control lot ID for convenience.
     *
     * @return The control lot ID
     */
    public String getControlLotId() {
        return result != null ? result.getControlLotId() : null;
    }
}
