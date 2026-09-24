package org.openelisglobal.qaevent.criticalcallback.bean;

/**
 * OGC-714 read side — Critical Callback Compliance summary for one date window.
 * When {@code enabled} is false the counts are zero and percentages null: the
 * indicator is opt-in and the service short-circuits before any query.
 */
public class CallbackSummaryResponse {

    private boolean enabled;
    private long criticalCount;
    private long confirmedCount;
    /** Percent, 2 decimals; null when no critical results in the window. */
    private Double compliancePercent;
    /** The qi_config %-goal for the CALLBACK indicator; null if unset. */
    private Double target;
    /** The configured callback target window, so surfaces can state it. */
    private int slaMinutes;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(long criticalCount) {
        this.criticalCount = criticalCount;
    }

    public long getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(long confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public Double getCompliancePercent() {
        return compliancePercent;
    }

    public void setCompliancePercent(Double compliancePercent) {
        this.compliancePercent = compliancePercent;
    }

    public Double getTarget() {
        return target;
    }

    public void setTarget(Double target) {
        this.target = target;
    }

    public int getSlaMinutes() {
        return slaMinutes;
    }

    public void setSlaMinutes(int slaMinutes) {
        this.slaMinutes = slaMinutes;
    }
}
