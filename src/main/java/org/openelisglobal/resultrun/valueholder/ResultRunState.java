package org.openelisglobal.resultrun.valueholder;

/**
 * OGC-1200 FR-A3. Open and Held runs still have rows to work; Complete and
 * Superseded runs are history.
 */
public enum ResultRunState {
    OPEN, HELD, COMPLETE, SUPERSEDED;

    public boolean isActive() {
        return this == OPEN || this == HELD;
    }
}
